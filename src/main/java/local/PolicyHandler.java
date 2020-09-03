package local;

import local.config.kafka.KafkaProcessor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolicyHandler{

    @Autowired
    PreQuestionRepository preQuestionRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReservationCompleted_preQuestionSend(@Payload ReservationCompleted reservationCompleted){

        // kafka-msg sub
        if(reservationCompleted.isMe()){
            //  예약 확정으로 인한 사전 문진표 발송
            System.out.println("##### 예약 확정으로 인한 사전 문진표 발송: " + reservationCompleted.toJson());
            if (reservationCompleted.getCustNm().equals("TEST")) {
                Canceled canceled = new Canceled();
                BeanUtils.copyProperties(this, canceled);
                canceled.publishAfterCommit();
            }

            // 동기식 테스트를 위해서 기존에 있는 호출을 그대로 이용
            // 검진 요청함 ( Req / Res : 동기 방식 호출)
            local.external.Hospital hospital = new local.external.Hospital();
            hospital.setHospitalId( Long.parseLong( reservationCompleted.getHospitalId()) );
            // mappings goes here
            PreQuestionApplication.applicationContext.getBean(local.external.HospitalService.class)
                    .screeningRequest(hospital.getHospitalId(),hospital);

            PreQuestion temp = new PreQuestion();
            temp.setScreeningId(reservationCompleted.getId());
            temp.setStatus("REQUEST_COMPLETED");
            temp.setCustNm(reservationCompleted.getCustNm());
            temp.setQuestionMsg(reservationCompleted.getCustNm()+"님, 사전문진표 발송 !!!");
            preQuestionRepository.save(temp);

        }
    }
    /*@StreamListener(KafkaProcessor.INPUT)
    public void wheneverHospitalDeleted_ForcedReservationCancel(@Payload HospitalDeleted hospitalDeleted){

        if(hospitalDeleted.isMe()){
            System.out.println("##### listener ForcedReservationCanceled : " + hospitalDeleted.toJson());
            //  병원일정 삭제로 인한 , 스케쥴 상태 변경
            List<PreQuestion> list = preQuestionRepository.findByHospitalId(String.valueOf(hospitalDeleted.getId()));
            for(PreQuestion temp : list){
                if(!"CANCELED".equals(temp.getStatus())) {
                    temp.setStatus("FORCE_CANCELED");
                    preQuestionRepository.save(temp);
                }
            }
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceled_ReservationCancel(@Payload Canceled canceled){

        if(canceled.isMe()){
            //  검진예약 취소로 인한 취소
            PreQuestion temp = preQuestionRepository.findByScreeningId(canceled.getId());
            temp.setStatus("CANCELED");
            preQuestionRepository.save(temp);

        }
    }*/

}
