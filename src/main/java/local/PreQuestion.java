package local;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="PreQuestion_table")
public class PreQuestion {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long screeningId;
    private String status;
    private String custNm;
    private String questionMsg;

    @PostPersist
    public void onPostPersist(){
        PreQuestionSent preQuestionSent = new PreQuestionSent();
        BeanUtils.copyProperties(this, preQuestionSent);
        preQuestionSent.publishAfterCommit();
    }

  /*  @PostUpdate
    public void onPostUpdate(){
        ReservationChanged reservationChanged = new ReservationChanged();
        BeanUtils.copyProperties(this, reservationChanged);
        reservationChanged.publishAfterCommit();
    }*/


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScreeningId() {
        return screeningId;
    }
    public void setScreeningId(Long screeningId) {
        this.screeningId = screeningId;
    }

    public String getCustNm() {
        return custNm;
    }
    public void setCustNm(String custNm) {
        this.custNm = custNm;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getQuestionMsg() { return questionMsg; }
    public void setQuestionMsg(String questionMsg) { this.questionMsg = questionMsg; }




}
