package com.example.demo;

import com.example.demo.distributedTransaction.bean.BaseBean;
import com.example.demo.distributedTransaction.rpc.external.TestExternal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

@SpringBootTest
@TestPropertySource(locations={"classpath:/application1.properties"})
class DemoApplicationTests {

	@Resource
	private TestExternal testExternal;

	@Test
	public void test1() {
		System.out.println("test");
		System.out.println(testExternal);
		testExternal.execute();
	}
}
