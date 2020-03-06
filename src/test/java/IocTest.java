import com.lagou.edu.dao.AccountDao;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class IocTest {

    @Test
    public void testIoc(){

        //通过读取classpath下的xml文件来启动容器
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:applicationCentext.xml");
        AccountDao accountDao = (AccountDao) applicationContext.getBean("accountDao");
        System.out.println(accountDao);
    }

}
