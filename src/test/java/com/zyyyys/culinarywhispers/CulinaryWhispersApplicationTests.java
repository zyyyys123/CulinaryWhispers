package com.zyyyys.culinarywhispers;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.zyyyys.culinarywhispers.module.search.repository.RecipeSearchRepository;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:culinary_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=never",
        "cw.token.store=memory",
        "cw.search.type=db",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration"
})
class CulinaryWhispersApplicationTests {

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private ElasticsearchOperations elasticsearchOperations;

    @MockBean
    private RecipeSearchRepository recipeSearchRepository;

    @Test
    void contextLoads() {
    }

}
