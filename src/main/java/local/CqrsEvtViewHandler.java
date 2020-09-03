package local;

import local.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CqrsEvtViewHandler {


    @Autowired
    private CqrsRepository cqrsRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenRequested_then_CREATE_1 (@Payload Requested requested) {
        try {
            if (requested.isMe()) {
                // view 객체 생성
                CqrsEvt myPage = new CqrsEvt();
                // view 객체에 이벤트의 Value 를 set 함
                myPage.setScreeningId(requested.getId());
                myPage.setCustNm(requested.getCustNm());
                myPage.setHospitalNm(requested.getHospitalNm());
                myPage.setStatus(requested.getStatus());
                // view 레파지 토리에 save
                cqrsRepository.save(myPage);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenCanceled_then_UPDATE_1(@Payload Canceled canceled) {
        try {
            if (canceled.isMe()) {
                // view 객체 조회
                List<CqrsEvt> cqrsEvtList = cqrsRepository.findByScreeningId(canceled.getId());
                for(CqrsEvt cqrsEvt : cqrsEvtList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    cqrsEvt.setStatus(canceled.getStatus());
                    // view 레파지 토리에 save
                    cqrsRepository.save(cqrsEvt);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}