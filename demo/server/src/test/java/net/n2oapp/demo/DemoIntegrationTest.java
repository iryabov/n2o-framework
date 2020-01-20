package net.n2oapp.demo;

import net.n2oapp.demo.model.ProtoPage;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;

import static com.codeborne.selenide.Configuration.browser;
import static com.codeborne.selenide.Configuration.headless;
import static com.codeborne.selenide.Selenide.open;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DemoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DemoIntegrationTest {

    @LocalServerPort
    private int port;

    private ProtoPage protoPage;

    @BeforeClass
    public static void configure() {
        browser = "chrome";
        headless = true;
    }

    @Before
    public void openProtoPage() {
        protoPage = open("http://localhost:" + port, ProtoPage.class);
    }

    @Test
    @Primary
    public void checkAllElementsExists() {
        protoPage.checkAllElementsExists();
    }

    @Test
    public void testGender() {
        protoPage.assertGender();
    }

    @Test
    public void testSorting() {
        protoPage.assertSorting();
    }

    @Test
    public void testAddClient() {
        protoPage.assertAddClient();
    }

    @Test
    public void testCreateClient() {
        protoPage.assertCreateClient();
    }

    @Test
    public void testClientUpdateFromModal() {
        protoPage.assertClientUpdateFromModal();
    }
}
