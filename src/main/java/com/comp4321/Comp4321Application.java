package com.comp4321;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jackson.JsonObjectDeserializer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@SpringBootApplication
@RestController
public class Comp4321Application {

	public static void main(String[] args) {
		SpringApplication.run(Comp4321Application.class, args);
	}

	@RequestMapping("/")
	@ResponseBody
	public Resource index(){
		return new ClassPathResource("static/index.html");
	}

	@PostMapping("/search")
	@ResponseBody
	public String handleQuery(@RequestBody String query){
		HashMap<String,String> result;
		try {
			Crawler crawler = new Crawler(
					"https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm",
					300,
					"stopwords.txt");
			crawler.crawl();

			Scoring score = new Scoring();
			score.score(query);

			result = SearchResult.getResults(50);
			return result.toString();
		} catch (Exception e) {
			e.printStackTrace();
			result = new HashMap<>();
			result.put("success","false");
			return result.toString();
		}
	}

}
