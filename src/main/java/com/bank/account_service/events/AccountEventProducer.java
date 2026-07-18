package com.bank.account_service.events;


import org.springframework.stereotype.Component;

@Component
public class AccountEventProducer {

//    private static final String TOPIC = "account.balance.changed";
//
//    private final KafkaTemplate<String, BalanceChangedEvent> kafkaTemplate;
//
//    public AccountEventProducer(KafkaTemplate<String, BalanceChangedEvent> kafkaTemplate) {
//        this.kafkaTemplate = kafkaTemplate;
//    }
//
//    public void publishBalanceChanged(BalanceChangedEvent event) {
//        // key = accountId ensures all events for one account land on the same partition,
//        // so a consumer sees them in order
//        kafkaTemplate.send(TOPIC, event.accountId().toString(), event);
//    }
}
