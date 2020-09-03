
package local;

public class HospitalDeleted extends AbstractEvent {

    private Long id;
    private String hospitalNm;
    private Long pCnt;
    private String chkDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getHospitalNm() {
        return hospitalNm;
    }

    public void setHospitalNm(String hospitalNm) {
        this.hospitalNm = hospitalNm;
    }
    public Long getPCnt() {
        return pCnt;
    }

    public void setPCnt(Long pCnt) {
        this.pCnt = pCnt;
    }
    public String getChkDate() {
        return chkDate;
    }

    public void setChkDate(String chkDate) {
        this.chkDate = chkDate;
    }
}
