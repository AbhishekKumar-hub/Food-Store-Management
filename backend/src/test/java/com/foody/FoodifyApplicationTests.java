package com.foody;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.data.mongodb.uri=mongodb://localhost:27017/foodify_test")
class FoodifyApplicationTests {

	@Test
	void contextLoads() {
	}

}
