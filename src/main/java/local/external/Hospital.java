package local.external;

public class Hospital {

    private Long id;
    private Long hospitalId;
    private String hospitalNm;
    private Long pCnt;
    private String chkDate;

    private String custNm;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getHospitalId() {
        return hospitalId;
    }
    public void setHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
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
