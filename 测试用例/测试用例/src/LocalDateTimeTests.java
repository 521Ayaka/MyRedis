import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class LocalDateTimeTests {


    @Test
    void DateObjectTest(){

        LocalDate date1 = LocalDate.now();
        LocalTime date2 = LocalTime.now();
        LocalDateTime date3 = LocalDateTime.now();

        System.out.println("date1: " + date1);
        System.out.println("date2: " + date2);
        System.out.println("date3: " + date3);

        /**
         * date1: 2022-10-10
         * date2: 15:49:48.946091
         * date3: 2022-10-10T15:49:48.946108
         */
    }


    /**
     * 基于时间
     */
    @Test
    void DateOfTest(){
        LocalDate date1 = LocalDate.of(2024, 1, 12);
        LocalTime date2 = LocalTime.of(12, 30, 40);
        LocalDateTime date3 = LocalDateTime.of(2024, 1, 12,12, 30, 40);

        System.out.println(date1);
        System.out.println(date2);
        System.out.println(date3);
        /**
         * 注意ISO 8601规定的日期和时间分隔符是T。标准格式如下：
         *
         * 日期：yyyy-MM-dd
         * 时间：HH:mm:ss
         * 带毫秒的时间：HH:mm:ss.SSS
         * 日期和时间：yyyy-MM-dd'T'HH:mm:ss
         * 带毫秒的日期和时间：yyyy-MM-dd'T'HH:mm:ss.SSS
         */
    }

}