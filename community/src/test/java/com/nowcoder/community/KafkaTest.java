package com.nowcoder.community;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class KafkaTest {
    @Autowired
    KafkaProducer kafkaProducer;
    @Autowired
    KafkaConsumer kafkaConsumer;
    @Test
    public void testKafka(){
        kafkaProducer.sendMessage("test","你好！");
        kafkaProducer.sendMessage("test","在吗？");

        try {
            Thread.sleep(10*1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
    @Component
    class KafkaProducer{
        @Autowired
        private KafkaTemplate kafkaTemplate;

        public void sendMessage(String topic,String content){
            //姓名-学号-实验报告1
            kafkaTemplate.send(topic,content);

        }
    }
    @Component
    class KafkaConsumer{
        @KafkaListener(topics = {"test"})
        public void handleMessage(ConsumerRecord consumerRecord){
            System.out.println(consumerRecord.value());
        }

    }


