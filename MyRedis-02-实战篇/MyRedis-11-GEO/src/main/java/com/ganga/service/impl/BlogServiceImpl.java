package com.ganga.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ganga.dto.ScoreResult;
import com.ganga.dto.UserDTO;
import com.ganga.entity.Follow;
import com.ganga.entity.User;
import com.ganga.service.IUserService;
import com.ganga.utils.SystemConstants;
import com.ganga.dto.Result;
import com.ganga.entity.Blog;
import com.ganga.mapper.BlogMapper;
import com.ganga.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ganga.service.IFollowService;
import com.ganga.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ganga.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private IFollowService followService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = this.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            // 封装blog
            queryUserBlog(blog);
            // 判断用户是否点赞
            isLiked(blog);
        });


        return Result.ok(records);
    }


    /**
     * 根据id查询博客
     * @param id
     * @return
     */
    @Override
    public Result queryByIdBlog(Long id) {
        //根据id查询博客
        Blog blog = this.getById(id);
        if (ObjectUtil.isEmpty(blog)){
            return Result.fail("博客不存在！");
        }
        //封装后的 blog 对象
        this.queryUserBlog(blog);
        // 判断用户是否点赞
        isLiked(blog);
        return Result.ok(blog);
    }

    /**
     * 更改点赞
     * @param id
     * @return
     */
    @Override
    public Result likeBlog(Long id) {

        //1.获取用户 id
        String userId = UserHolder.getUser().getId().toString();
        String key = BLOG_LIKED_KEY + id;

        //2.判断当前用户是否点赞
        //Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId);
        Double isMemberScore = stringRedisTemplate.opsForZSet().score(key, userId);
        if (isMemberScore == null){
            //3.未点赞
            //3.1 数据库点赞数 +1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            //3.2 redis中 添加zset 成员
            if (isSuccess){
                //stringRedisTemplate.opsForSet().add(key,userId);
                stringRedisTemplate.opsForZSet().add(key,userId,System.currentTimeMillis()); //key value score
            }
        }else{
            //4.未点赞
            //4.1 数据库点赞数 -1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            //4.2 redis中 删除zset 成员
            if (isSuccess){
                //stringRedisTemplate.opsForSet().remove(key,userId);
                stringRedisTemplate.opsForZSet().remove(key,userId);
            }
        }

        return Result.ok();
    }

    /**
     * 根据博客id 获取点赞排行榜前 5 名 <br>
     *      注意：                   <br>
     *      有一个坑:                <br>
     *      select * from tb_user where id in(3,2,1); 的查询结果顺序 是 1 2 3     <br>
     *      select * from tb_user where id in(3,2,1) order by field(id,3,2,1)  <br>
     *      这样才能保证按给定的顺序查询
     *
     * @param id
     * @return
     */
    @Override
    public Result likesBlogTop(Long id) {
        String key = BLOG_LIKED_KEY + id.toString();
        //根据博客id查询点赞前 5 名
        Set<String> range = stringRedisTemplate.opsForZSet().range(key, 0, 4);// top5
        if (range == null){
            return Result.ok();
        }
        List<Long> ids = range.stream().map(Long::valueOf).collect(Collectors.toList());
        if (ObjectUtil.isEmpty(ids)){
            return Result.ok();
        }
        String idStr = StrUtil.join("," , ids);
        //根据这些用户 id 获取用户信息
        List<UserDTO> userDTOS = userService
                //.listByIds(ids)
                .query()
                .in("id",ids)
                .last("order by field(id,"+ idStr + ")")
                .list()
                .stream()//封装成 UserDTO 防止敏感信息泄露
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOS);
    }

    @Override
    public Result queryOfUserBlog(Long id, Integer current) {
        //根据用户查询blog
        Page<Blog> page = query()
                .eq("user_id", id)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        //获取当前页面数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @Override
    public Result queryMyBlog(Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 保存博客 并 推送
     * @param blog
     * @return
     */
    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean isSuccess = save(blog);
        if (!isSuccess){
            return Result.fail("新增笔记失败！");
        }

        // 推送给关注的 用户 Feet流 推模式
        //1.获取粉丝id
        List<Follow> follows = followService.query().eq("follow_user_id", UserHolder.getUser().getId()).list();
        //2.推送到邮箱当中
        for (Follow follow: follows) {
            //3.以 SortedSet 类型存储
            String key = FEED_USER_KEY + follow.getUserId();
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }

        // 返回 id
        return Result.ok(blog.getId());
    }

    /**
     * 分页查询当前用户 关注用户的博客
     * @param maxTime
     * @param offset
     * @return
     */
    @Override
    public Result queryOfFollow(Long maxTime, Integer offset) {
        //1.获取用户id
        Long userId = UserHolder.getUser().getId();
        //2.从Redis用户信箱中取出 关注用的发布的博客
        String key = FEED_USER_KEY + userId.toString();
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate
                        .opsForZSet()
                        .reverseRangeByScoreWithScores(key, 0, maxTime, offset, 2);
        //3.判断邮件是否为空
        if (ObjectUtil.isEmpty(typedTuples)){
            return Result.ok();
        }

        //4.进行解析 博客id集合 当前页最小时间戳:minTime 最小时间戳出现的次数
        ArrayList<Long> blogIds = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int minCount = 1;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            //关注的博客id
            Long blogId = Long.valueOf(tuple.getValue());
            blogIds.add(blogId);
            //获取最小分数 和 最小分数个数
            long score = tuple.getScore().longValue();
            if (score == minTime){
                minCount ++;
            }else{
                minTime = score;
                minCount = 1;
            }
        }

        //5.根据 博客id集合 查询博客对象
        String idStr = StrUtil.join(",", blogIds);
        List<Blog> blogs = query()
                .in("id", blogIds)
                .last("order by field(id," + idStr + ")")
                .list();
        //给博客封装非数据库字段
        blogs.forEach(blog -> {
            //封装后的 blog 对象
            queryUserBlog(blog);
            // 判断用户是否点赞
            isLiked(blog);
        });

        //6.封装 ScoreResult 对象
        ScoreResult scoreResult = new ScoreResult();
        scoreResult.setList(blogs);
        scoreResult.setMinTime(minTime);
        scoreResult.setOffset(minCount);

        //7.返回个前端
        return Result.ok(scoreResult);
    }


    //为博客 并设置用户信息
    private void queryUserBlog(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }


    //判断当前用户是否为当前博客点赞
    private void isLiked(Blog blog){
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null){
            //用户未登录，不需要查询是否点赞过
            return;
        }
        String userId = userDTO.getId().toString();
        String key = BLOG_LIKED_KEY + blog.getId().toString();
        //Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        Double isMember = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        //封装blog
        blog.setIsLike(isMember != null);
    }

}
