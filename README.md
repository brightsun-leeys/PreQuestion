# 4th-team3-health-screening (건강검진 예약 서비스) + 개인 micro service 추가 (사전문진표발송)

# repo
 1. 병원관리 : https://github.com/byounghoonmoon/HospitalManage.git    
    >>(Fork)>>>>  https://github.com/brightsun-leeys/HospitalManage.git
 1. 검진관리 : https://github.com/byounghoonmoon/ScreeningManage.git  
    >>(Fork)>>>>  https://github.com/brightsun-leeys/ScreeningManage.git
 1. 예약관리 : https://github.com/byounghoonmoon/ReservationManage.git 
    >>(Fork)>>>>  https://github.com/brightsun-leeys/ReservationManage.git
 1. 마이페이지 : https://github.com/byounghoonmoon/MyPage.git          
    >>(Fork)>>>>  https://github.com/brightsun-leeys/MyPage.git
 1. 게이트웨이 : https://github.com/byounghoonmoon/gateway.git         
    >>(Fork)>>>>  https://github.com/brightsun-leeys/gateway.git
 ```diff
 + 6. (NEW) 사전문진표 발송 : https://github.com/brightsun-leeys/PreQuestion.git
 ```
 
# Table of contents

- [서비스 시나리오](#서비스-시나리오)
  - [시나리오 테스트결과](#시나리오-테스트결과)
- [분석/설계](#분석설계)
- [구현](#구현)
  - [DDD 의 적용](#ddd-의-적용)
  - [Gateway 적용](#Gateway-적용)
  - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
  - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
  - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
- [운영](#운영)
  - [CI/CD 설정](#cicd설정)
  - [서킷 브레이킹 / 장애격리](#서킷-브레이킹-/-장애격리)
  - [오토스케일 아웃](#오토스케일-아웃)
  - [무정지 재배포](#무정지-재배포)
  

# 서비스 시나리오

## 기능적 요구사항
1. 관리자가 병원 정보( 병원이름, 예약일, 가능인원수)를 등록한다.
1. 고객이 건강검진을 예약을 요청한다. 
1. 고객의 검진 요청에 따라서 해당 병원의 검진가능 인원이 감소한다. (Sync) 
1. 고객의 건강검진 예약 상태가 예약 완료로 변경된다. (Async) 
1. 고객의 검진 예약 완료에 따라서 예약관리의 해당 내역의 상태가 등록된다.
1. 고객이 건강검진 예약을 취소한다.
1. 고객의 예약 취소에 따라서 병원의 검진가능 인원이 증가한다. (Async)
1. 고객의 예약 취소에 따라서 예약관리의 해당 내역의 상태가 예약 취소로 변경된다.
1. 관리자가 병원 정보를 삭제한다.
1. 관리자의 병원 정보 삭제에 따라서 해당 병원에 예약한 예약자의 상태를 변경한다.
1. 관리자의 병원 정보 삭제에 따라서 예약관리의 해당 내역의 상태가 예약 강제 취소로 변경된다.
1. 사용자가 건강검진 예약내역 상태를 조회한다.
```diff
+ 13. 고객 예약 완료시, 사전문진표를 발송한다.
```

## 비기능적 요구사항
1. 트랜잭션
    1. 고객의 예약에 따라서 해당 날짜 / 병원의 검진가능 인원이 감소한다. > Sync
    1. 고객의 취소에 따라서 해당 날짜 / 병원의 검진가능 인원이 증가한다. > Async
1. 장애격리
    1. 예약 관리 서비스에 장애가 발생하더라도 검진 예약은 정상적으로 처리 가능하다.  > Async (event-driven)
    1. 서킷 브레이킹 프레임워크 > istio-injection + DestinationRule
1. 성능
    1. 고객은 본인의 예약 상태 및 이력 정보를 확인할 수 있다. > CQRS


# 분석/설계

## AS-IS 조직 (Horizontally-Aligned)
  ![1](https://user-images.githubusercontent.com/67453893/91927140-baa8af00-ed13-11ea-8ead-0d4616a6da56.png)

## TO-BE 조직 (Vertically-Aligned)
![그림1](https://user-images.githubusercontent.com/67453893/92060798-a2966580-edcf-11ea-8a0c-448c4d1dc3c4.png)

## Event Storming 결과

![그림7](https://user-images.githubusercontent.com/67453893/92063643-9d88e480-edd6-11ea-8b75-42f712d045b7.png)

### 이벤트 도출
![그림2](https://user-images.githubusercontent.com/67453893/91927264-065b5880-ed14-11ea-8c93-fe8b68396363.png)

### 부적격 이벤트 탈락
![그림3](https://user-images.githubusercontent.com/67453893/91927289-0f4c2a00-ed14-11ea-846b-1f7ae4d67378.png)

    - 과정중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함

### 액터, 커맨드 부착하여 읽기 좋게
![그림4](https://user-images.githubusercontent.com/67453893/91927348-39055100-ed14-11ea-8711-a3cfa135d104.png)

### 어그리게잇으로 묶기
![그림5](https://user-images.githubusercontent.com/67453893/91927353-3acf1480-ed14-11ea-8aa2-da1ded8fd962.png)


### 바운디드 컨텍스트로 묶기

![그림6](https://user-images.githubusercontent.com/67453893/91927551-c648a580-ed14-11ea-99e8-63422e9a09c5.png)

    - 도메인 서열 분리 
        - Core Domain:  검진관리 : 없어서는 안될 핵심 서비스이며, 연견 Up-time SLA 수준을 99.999% 목표, 배포주기는  1주일 1회 미만
        - Supporting Domain:   병원관리 : 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 80% 이상 uptime 목표, 배포주기는 각 팀의 자율이나 표준 스프린트 주기가 1주일 이므로 1주일 1회 이상을 기준으로 함.
        - General Domain:   예약관리 : 단순 이력을 관리하기 위한 서비스, (NEW) 사전문진관리 : 사전문진표를 전송, 향후 외부시스템으로 

### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)

![그림7](https://user-images.githubusercontent.com/67453893/91927554-c6e13c00-ed14-11ea-96fa-99b7b8f40cfe.png)

### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)

![그림8](https://user-images.githubusercontent.com/67453893/91927556-c779d280-ed14-11ea-8a3e-125d77f6b75d.png)

```
# 도메인 서열
- Core : Screening
- Supporting : Hospital
- General : Reservation
```

### 완성본에 대한 기능적/비기능적 요구사항을 커버하는지 검증
![그림9](https://user-images.githubusercontent.com/67453893/91927779-525acd00-ed15-11ea-9ad8-4141fb72c470.png)
![그림10](https://user-images.githubusercontent.com/67453893/91927763-4e2eaf80-ed15-11ea-9211-0389e6665436.png)
![그림11](https://user-images.githubusercontent.com/67453893/91927770-4f5fdc80-ed15-11ea-8010-685f2fe7cc4e.png)
![그림12](https://user-images.githubusercontent.com/67453893/91927777-50910980-ed15-11ea-8725-a20f855533f4.png)
![그림13](https://user-images.githubusercontent.com/67453893/91927778-51c23680-ed15-11ea-83db-3b1a8a3fc4d7.png)

## 헥사고날 아키텍처 다이어그램 도출

* CQRS 를 위한 Mypage 서비스만 DB를 구분하여 적용
![그림3](https://user-images.githubusercontent.com/67453893/92060809-a7f3b000-edcf-11ea-805d-55a1bcc848e0.png)


# 구현

## 시나리오 테스트결과

| 기능 | 이벤트 Payload |
|---|:---:|
| 1.관리자가 병원 정보( 병원이름, 예약일, 가능인원수)를 등록한다. |![image](https://user-images.githubusercontent.com/25805562/91837451-3577b880-ec87-11ea-88b1-dc4e9d74790d.png)|
| 2.고객이 건강검진을 예약을 요청한다. </br>3.해당 병원의 검진가능 인원이 감소한다. (Sync)</br>4.예약 완료로 변경된다. (Async)</br>5.예약관리의 해당 내역의 상태가 등록된다.</br>(신규) 13.고객 예약 완료시, 사전문진표를 발송한다. |![image](https://user-images.githubusercontent.com/25805562/91837806-7a035400-ec87-11ea-8966-09403bd5e7eb.png)</br></br>![그림4](https://user-images.githubusercontent.com/67453893/92061046-48e26b00-edd0-11ea-9f91-89537cf86d31.png) |
| 6.고객이 건강검진 예약을 취소한다.</br>7.취소 시, 병원의 검진가능 인원이 증가한다. (Async)</br>8.예약관리의 해당 내역의 상태가 예약 취소로 변경된다. | ![image](https://user-images.githubusercontent.com/25805562/91837990-c2227680-ec87-11ea-9fb1-530410922532.png) |
| 9.관리자가 병원 정보를 삭제한다.</br>10.해당 병원에 예약한 예약자의 상태를 예약 강제 취소 변경한다. (Async)</br>11.예약관리의 해당 내역의 상태가 예약 강제 취소로 변경된다. | ![image](https://user-images.githubusercontent.com/25805562/91838119-f007bb00-ec87-11ea-9edd-38d9963f9ee0.png) | 
| 12.건강검진 예약내역 상태를 조회한다.| ![image](https://user-images.githubusercontent.com/25805562/91838415-6ad0d600-ec88-11ea-9df8-1c6895fe6d75.png) |


## DDD 의 적용

분석/설계 단계에서 도출된 MSA는 총 5개로 아래와 같다.
* MyPage 는 CQRS 를 위한 서비스

| MSA | 기능 | port | 조회 API | Gateway 사용시 |
|---|:---:|:---:|---|---|
| Screening | 검진 관리 | 8081 | http://localhost:8081/screenings | http://ScreeningManage:8080/screenings |
| Hospital | 병원 관리 | 8082 | http://localhost:8082/hospitals | http://HospitalManage:8080/hospitals |
| Reservation | 예약 관리 | 8083 | http://localhost:8083/reservations | http://ReservationManage:8080/reservations |
| MyPage | my page | 8084 | http://localhost:8084/myPages | http://MyPage:8080/myPages |
| PreQuestion | 사전문진표 발송 | 8085 | http://localhost:8085/preQuestion | http://PreQuestion:8080/preQuestion |



## Gateway 적용

```
spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: ScreeningManage
          uri: http://ScreeningManage:8080
          predicates:
            - Path=/screenings/**
        - id: HospitalManage
          uri: http://HospitalManage:8080
          predicates:
            - Path=/hospitals/** 
        - id: ReservationManage
          uri: http://ReservationManage:8080
          predicates:
            - Path=/reservations/** 
        - id: MyPage
          uri: http://MyPage:8080
          predicates:
            - Path= /myPages/**

(아래와 같이 변경)
```
![그림6](https://user-images.githubusercontent.com/67453893/92062445-ba6fe880-edd3-11ea-8755-17d7e39dce85.png)

## 폴리글랏 퍼시스턴스

CQRS 를 위한 Mypage 서비스만 DB를 구분하여 적용함. 인메모리 DB인 hsqldb 사용.

```
pom.xml 에 적용
<!-- 
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>runtime</scope>
		</dependency>
 -->
		<dependency>
		    <groupId>org.hsqldb</groupId>
		    <artifactId>hsqldb</artifactId>
		    <version>2.4.0</version>
		    <scope>runtime</scope>
		</dependency>
```


## 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 고객검진요청(Screening)->병원정보관리(Hospital) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 
고객검진요청 > 병원정보관리 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리
- FeignClient 서비스 구현

```
# HospitalService.java

@FeignClient(name="HospitalManage", url="${api.hospital.url}")
public interface HospitalService {

    @RequestMapping(method= RequestMethod.PUT, value="/hospitals/{hospitalId}", consumes = "application/json")
    public void screeningRequest(@PathVariable("hospitalId") Long hospitalId, @RequestBody Hospital hospital);

}
```

- 고객검진요청을 받은 직후(@PostPersist) 병원벙보를 요청하도록 처리
```
# Screening.java (Entity)

    @PostPersist
    public void onPostPersist(){;

        // 검진 요청함 ( Req / Res : 동기 방식 호출)
        local.external.Hospital hospital = new local.external.Hospital();
        hospital.setHospitalId(getHospitalId());
        ScreeningManageApplication.applicationContext.getBean(local.external.HospitalService.class)
            .screeningRequest(hospital.getHospitalId(),hospital);

        Requested requested = new Requested();
        BeanUtils.copyProperties(this, requested);
        requested.publishAfterCommit();
    }
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 병원관리 시스템이 장애가 나면 검진요청 못받는다는 것을 확인


```
#병원정보관리(Hospital) 서비스를 잠시 내려놓음 (ctrl+c)

#고객검진요청
http POST localhost:8081/screenings hospitalId=1 hospitalNm="Samsung" custNm="MOON" chkDate="20200910" status="REQUESTED"   #Fail
http POST localhost:8081/screenings hospitalId=2 hospitalNm="SK" custNm="JUNG" chkDate="20200911" status="REQUESTED"   #Fail

#병원정보관리 재기동
cd HospitalManage
mvn spring-boot:run

#고객검진요청 처리
http POST localhost:8081/screenings hospitalId=1 hospitalNm="Samsung" custNm="MOON" chkDate="20200910" status="REQUESTED"   #Success
http POST localhost:8081/screenings hospitalId=2 hospitalNm="SK" custNm="JUNG" chkDate="20200911" status="REQUESTED"   #Success
```

- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, Fallback 처리는 운영단계에서 설명한다.)




## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트


고객검진취소가 이루어진 후에 예약시스템으로 이를 알려주는 행위는 동기식이 아니라 비 동기식으로 처리하였다.
 
- 이를 위하여 고객검진신청이력에 기록을 남긴 후에 곧바로 검진취소신청 되었다는 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
package local;

@Entity
@Table(name="Screening_table")
public class Screening {

 ...
    @PostUpdate
    public void onPostUpdate(){

        System.out.println("#### onPostUpdate :" + this.toString());

        if("CANCELED".equals(this.getStatus())) {
            Canceled canceled = new Canceled();
            BeanUtils.copyProperties(this, canceled);
            canceled.publishAfterCommit();
        }
	}
}
```
- 예약관리 서비스에서는 검진취소신청 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다

```
package local;

...

@Service
public class PolicyHandler{

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceled_ReservationCancel(@Payload Canceled canceled){

        if(canceled.isMe()){
            //  검진예약 취소로 인한 취소
            Reservation temp = reservationRepository.findByScreeningId(canceled.getId());
            temp.setStatus("CANCELED");
            reservationRepository.save(temp);

        }
    }

}

```

예약관리 시스템은 병원/검진신청과 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 예약관리시스템이 유지보수로 인해 잠시 내려간 상태라도 예약신청을 받는데 문제가 없다.

```
#예약관리 서비스 (Reservation) 를 잠시 내려놓음 (ctrl+c)

#검진신청취소처리
http PUT localhost:8081/screenings hospitalId=1 hospitalNm="Samsung" chkDate= "20200907" custNm= "Moon44" status= "Canceled"   #Success
http PUT localhost:8081/screenings hospitalId=2 hospitalNm="SK" chkDate= "20200908" custNm= "YOU" status= "Canceled"   #Success

#예약관리상태 확인
http localhost:8083/reservations     # 예약상태 안바뀜 확인

#예약관리 서비스 기동
cd ReservationManage
mvn spring-boot:run

#예약관리상태 확인
http localhost:8083/reservations     # 예약상태가 "취소됨"으로 확인
```

# 운영

## CI/CD 설정

각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD 플랫폼은 AWS CodeBuild를 사용하였으며, 
pipeline build script 는 각 프로젝트 폴더 이하에 buildspec.yml 에 포함되었다.
- CodeBuild 기반으로 CI/CD 파이프라인 구성
MSA 서비스별 CodeBuild 프로젝트 생성하여  CI/CD 파이프라인 구성

<img src="https://user-images.githubusercontent.com/67447253/91837032-92bf3a00-ec86-11ea-96e6-263a590cd849.JPG"/>

- Git Hook 연결
연결한 Github의 소스 변경 발생 시 자동으로 빌드 및 배포 되도록 Git Hook 연결 설정

<img src="https://user-images.githubusercontent.com/67447253/91837300-0103fc80-ec87-11ea-9698-fc1afb52893c.JPG" />


## 동기식 호출 / 서킷 브레이킹 / 장애격리

### 서킷 브레이킹 istio-injection + DestinationRule

* istio-injection 적용 (기 적용완료)
```
kubectl label namespace skcc-ns istio-injection=enabled
```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 100명
- 60초 동안 실시
```
$siege -c100 -t60S -r10  -v http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals 
HTTP/1.1 200   0.02 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.01 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.02 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.01 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.00 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.02 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.00 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.00 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.01 secs:    5650 bytes ==> GET  /hospitals
```
* 서킷 브레이킹을 위한 DestinationRule 적용
```
#dr-hospital.yaml
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: dr-hospital
  namespace: skcc-ns
spec:
  host: hospitalmanage
  trafficPolicy:
    connectionPool:
      http:
        http1MaxPendingRequests: 1
        maxRequestsPerConnection: 1
    outlierDetection:
      interval: 1s
      consecutiveErrors: 2
      baseEjectionTime: 10s
      maxEjectionPercent: 100
```


```
$kubectl apply -f dr-hospital.yaml

$siege -c100 -t60S -r10  -v http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals 
HTTP/1.1 200   0.03 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.04 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.02 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      81 bytes ==> GET  /hospitals
HTTP/1.1 200   0.01 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      81 bytes ==> GET  /hospitals
HTTP/1.1 200   0.01 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.01 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.01 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.01 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      81 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      81 bytes ==> GET  /hospitals
HTTP/1.1 200   0.02 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.02 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.01 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      81 bytes ==> GET  /hospitals
HTTP/1.1 503   0.02 secs:      95 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      95 bytes ==> GET  /hospitals
HTTP/1.1 200   0.03 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 200   0.02 secs:    5650 bytes ==> GET  /hospitals
HTTP/1.1 503   0.00 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.00 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.02 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.02 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.02 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.02 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.02 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.00 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.00 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.02 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.00 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.00 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.00 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.00 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.01 secs:      19 bytes ==> GET  /hospitals
HTTP/1.1 503   0.00 secs:      19 bytes ==> GET  /hospitals

Transactions:                    194 hits
Availability:                  16.68 %
Elapsed time:                  59.76 secs
Data transferred:               1.06 MB
Response time:                  0.03 secs
Transaction rate:               3.25 trans/sec
Throughput:                     0.02 MB/sec
Concurrency:                    0.10
Successful transactions:         194
Failed transactions:             969
Longest transaction:            0.04
Shortest transaction:           0.00


```

* DestinationRule 적용되어 서킷 브레이킹 동작 확인 (kiali 화면)
![Kaili_DR RUlE적용](https://user-images.githubusercontent.com/67453893/91850567-b7bca880-ec98-11ea-8f9b-59b4223fb046.png)

* 다시 부하 발생하여 DestinationRule 적용 제거하여 정상 처리 확인
```
kubectl delete -f dr-hospital.yaml
```


### 오토스케일 아웃
앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 

* Metric Server 설치(CPU 사용량 체크를 위해)
```
$kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.3.6/components.yaml
$kubectl get deployment metrics-server -n kube-system
```

* (istio injection 적용한 경우) istio injection 적용 해제
```
kubectl label namespace skcc-ns istio-injection=disabled --overwrite
```

- Deployment 배포시 resource 설정 적용
```
    spec:
      containers:
          ...
          resources:
            limits:
              cpu: 500m 
            requests:
              cpu: 200m 
```

- replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다:
```
kubectl autoscale deploy hospitalmanage -n skcc-ns --min=1 --max=10 --cpu-percent=15

# 적용 내용
$kubectl get all -n skcc-ns
NAME                        TYPE           CLUSTER-IP       EXTERNAL-IP                                                              PORT(S)          AGE
service/gateway             LoadBalancer   10.100.95.162    a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com   8080:30387/TCP   19h
service/hospitalmanage      ClusterIP      10.100.5.64      <none>                                                                   8080/TCP         19h
service/mypage              ClusterIP      10.100.240.169   <none>                                                                   8080/TCP         19h
service/reservationmanage   ClusterIP      10.100.232.233   <none>                                                                   8080/TCP         19h
service/screeningmanage     ClusterIP      10.100.101.120   <none>                                                                   8080/TCP         19h

NAME                                READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/gateway             1/1     1            1           19h
deployment.apps/hospitalmanage      1/1     1            1           11h
deployment.apps/mypage              1/1     1            1           19h
deployment.apps/reservationmanage   1/1     1            1           19h
deployment.apps/screeningmanage     1/1     1            1           19h

NAME                                           DESIRED   CURRENT   READY   AGE
replicaset.apps/gateway-5d58bbcb67             1         1         1       19h
replicaset.apps/gateway-db44fcf75              0         0         0       19h
replicaset.apps/hospitalmanage-8658bbbb6f      1         1         1       11h
replicaset.apps/mypage-567c4b57ff              1         1         1       18h
replicaset.apps/mypage-f5486756b               0         0         0       19h
replicaset.apps/reservationmanage-6f47749879   0         0         0       18h
replicaset.apps/reservationmanage-c96669994    1         1         1       18h
replicaset.apps/reservationmanage-f74d47f65    0         0         0       19h
replicaset.apps/screeningmanage-56ff67c8cf     0         0         0       18h
replicaset.apps/screeningmanage-598b5f9767     0         0         0       17h
replicaset.apps/screeningmanage-645c457774     0         0         0       19h
replicaset.apps/screeningmanage-6485bb9857     0         0         0       17h
replicaset.apps/screeningmanage-6865764467     0         0         0       19h
replicaset.apps/screeningmanage-78984d5dc8     0         0         0       17h
replicaset.apps/screeningmanage-9498f6bdc      1         1         1       17h

NAME                                                 REFERENCE                   TARGETS         MINPODS   MAXPODS   REPLICAS   AGE
horizontalpodautoscaler.autoscaling/hospitalmanage   Deployment/hospitalmanage   2%/15%   1         10        0          7s
```

- siege로 워크로드를 1분 동안 걸어준다.
```
$  siege -c100 -t60S -r10  -v http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals
```

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:
```
kubectl get deploy hospitalmanage -n skcc-ns -w 
```

- 어느정도 시간이 흐른 후 (약 30초) 스케일 아웃이 벌어지는 것을 확인할 수 있다:
```
NAME             READY   UP-TO-DATE   AVAILABLE   AGE
hospitalmanage   1/1     1            1           11h
hospitalmanage   1/4     1            1           11h
hospitalmanage   1/4     1            1           11h
hospitalmanage   1/4     1            1           11h
hospitalmanage   1/4     4            1           11h
hospitalmanage   1/5     4            1           11h
hospitalmanage   1/5     4            1           11h
hospitalmanage   1/5     4            1           11h
hospitalmanage   1/5     5            1           11h
```

- kubectl get으로 HPA을 확인하면 CPU 사용률이 64%로 증가됐다.
```
$kubectl get hpa hospitalmanage -n skcc-ns 
NAME                                                 REFERENCE                   TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
horizontalpodautoscaler.autoscaling/hospitalmanage   Deployment/hospitalmanage   64%/15%   1         10        5          2m54s
```

- siege 의 로그를 보면 Availability가 100%로 유지된 것을 확인 할 수 있다.  
```
Lifting the server siege...
Transactions:                  26446 hits
Availability:                 100.00 %
Elapsed time:                 179.76 secs
Data transferred:               8.73 MB
Response time:                  0.68 secs
Transaction rate:             147.12 trans/sec
Throughput:                     0.05 MB/sec
Concurrency:                   99.60
Successful transactions:       26446
Failed transactions:               0
Longest transaction:            5.85
Shortest transaction:           0.00
```

- HPA 삭제 
```
$kubectl kubectl delete hpa hospitalmanage  -n skcc-ns
```


## 무정지 재배포

먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler, CB 설정을 제거함
Readiness Probe 미설정 시 무정지 재배포 가능여부 확인을 위해 buildspec.yml의 Readiness Probe 설정을 제거함

- seige 로 배포작업 직전에 워크로드를 모니터링 함.
```
$ siege -v -c1 -t240S --content-type "application/json" 'http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals POST {"id": "101","hospitalId":"2","hospitalNm":"bye","chkDate":"0909","pcnt":20}'

** SIEGE 4.0.4
** Preparing 100 concurrent users for battle.
The server is now under siege...

HTTP/1.1 200     0.48 secs:       0 bytes ==> POST http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals
HTTP/1.1 200     0.49 secs:       0 bytes ==> POST http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals
HTTP/1.1 200     0.63 secs:       0 bytes ==> POST http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals
HTTP/1.1 200     0.48 secs:       0 bytes ==> POST http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals
:

```

- CI/CD 파이프라인을 통해 새버전으로 재배포 작업함
Git hook 연동 설정되어 Github의 소스 변경 발생 시 자동 빌드 배포됨
재배포 작업 중 서비스 중단됨 (503 오류 발생)
```
HTTP/1.1 200     0.48 secs:       0 bytes ==> POST http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals
HTTP/1.1 200     0.55 secs:       0 bytes ==> POST http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals
HTTP/1.1 503     0.47 secs:      95 bytes ==> POST http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals
HTTP/1.1 503     0.48 secs:      95 bytes ==> POST http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals
HTTP/1.1 503     0.51 secs:      95 bytes ==> POST http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals
HTTP/1.1 503     0.47 secs:      95 bytes ==> POST http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals
HTTP/1.1 503     0.48 secs:      95 bytes ==> POST http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals
HTTP/1.1 503     0.53 secs:      95 bytes ==> POST http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals
HTTP/1.1 503     0.50 secs:      95 bytes ==> POST http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals
HTTP/1.1 503     0.45 secs:      95 bytes ==> POST http://a67fdf8668e5d4b518f8ac2a62bd4b45-334568913.us-east-2.elb.amazonaws.com:8080/hospitals
:

```

- seige 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인
```
Transactions:                    372 hits
Availability:                  90.29 %
Elapsed time:                 205.09 secs
Data transferred:               0.00 MB
Response time:                  0.55 secs
Transaction rate:               1.81 trans/sec
Throughput:                     0.00 MB/sec
Concurrency:                    1.00
Successful transactions:         372
Failed transactions:              40
Longest transaction:            1.50
Shortest transaction:           0.43

```
- 배포기간중 Availability 가 평소 100%에서 90% 대로 떨어지는 것을 확인. 
원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY 상태로 인식하여 서비스 유입을 진행한 것이기 때문으로 판단됨. 
이를 막기위해 Readiness Probe 를 설정함 (buildspec.yml의 Readiness Probe 설정)
```
# buildspec.yaml 의 Readiness probe 의 설정:
- CI/CD 파이프라인을 통해 새버전으로 재배포 작업함

readinessProbe:
    httpGet:
      path: '/actuator/health'
      port: 8080
    initialDelaySeconds: 30
    timeoutSeconds: 2
    periodSeconds: 5
    failureThreshold: 10
    
```

- 동일한 시나리오로 재배포 한 후 Availability 확인:
```
Transactions:                    234 hits
Availability:                 100.00 %
Elapsed time:                 119.04 secs
Data transferred:               0.00 MB
Response time:                  0.51 secs
Transaction rate:               1.97 trans/sec
Throughput:                     0.00 MB/sec
Concurrency:                    1.00
Successful transactions:         234
Failed transactions:               0
Longest transaction:            1.57
Shortest transaction:           0.41

```

배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.


## ConfigMap 사용

시스템별로 또는 운영중에 동적으로 변경 가능성이 있는 설정들을 ConfigMap을 사용하여 관리합니다.
Application에서 특정 도메일 URL을 ConfigMap 으로 설정하여 운영/개발등 목적에 맞게 변경가능합니다.  

* my-config.yaml
```
apiVersion: v1
kind: ConfigMap
metadata:
  name: my-config
  namespace: skcc-ns
data:
  api.hospital.url: http://HospitalManage:8080
```
my-config라는 ConfigMap을 생성하고 key값에 도메인 url을 등록한다. 

* ScreeningManage/buildsepc.yaml (configmap 사용)
```
 cat  <<EOF | kubectl apply -f -
        apiVersion: apps/v1
        kind: Deployment
        metadata:
          name: $_PROJECT_NAME
          namespace: $_NAMESPACE
          labels:
            app: $_PROJECT_NAME
        spec:
          replicas: 1
          selector:
            matchLabels:
              app: $_PROJECT_NAME
          template:
            metadata:
              labels:
                app: $_PROJECT_NAME
            spec:
              containers:
                - name: $_PROJECT_NAME
                  image: $AWS_ACCOUNT_ID.dkr.ecr.$_AWS_REGION.amazonaws.com/$_PROJECT_NAME:$CODEBUILD_RESOLVED_SOURCE_VERSION
                  ports:
                    - containerPort: 8080
                  env:
                    - name: api.hospital.url
                      valueFrom:
                        configMapKeyRef:
                          name: my-config
                          key: api.hospital.url
                  imagePullPolicy: Always
                
        EOF
```
Deployment yaml에 해단 configMap 적용

* HospitalService.java
```
@FeignClient(name="HospitalManage", url="${api.hospital.url}")//,fallback = HospitalServiceFallback.class)
public interface HospitalService {

    @RequestMapping(method= RequestMethod.PUT, value="/hospitals/{hospitalId}", consumes = "application/json")
    public void screeningRequest(@PathVariable("hospitalId") Long hospitalId, @RequestBody Hospital hospital);

}
```
url에 configMap 적용

* kubectl describe pod screeningmanage-9498f6bdc-qtclh  -n skcc-ns
```
Containers:
  screeningmanage:
    Container ID:   docker://8415f0125bac0264b5f77d14ed8ee7c28bc177e2cce9141a4c36e076c7920971
    Image:          052937454741.dkr.ecr.us-east-2.amazonaws.com/screeningmanage:f8102f4078683bdbf345cc5cae7983b1cb8ea                                                                      668
    Image ID:       docker-pullable://052937454741.dkr.ecr.us-east-2.amazonaws.com/screeningmanage@sha256:ebc8945df607                                                                      acc63d87e20d345e17245e3472fec43a9690e8ab9ca959573c9b
    Port:           8080/TCP
    Host Port:      0/TCP
    State:          Running
      Started:      Tue, 01 Sep 2020 07:55:29 +0000
    Ready:          True
    Restart Count:  0
    Liveness:       http-get http://:8080/actuator/health delay=120s timeout=2s period=5s #success=1 #failure=5
    Readiness:      http-get http://:8080/actuator/health delay=30s timeout=2s period=5s #success=1 #failure=10
    Environment:
      api.hospital.url:  <set to the key 'api.hospital.url' of config map 'my-config'>  Optional: false
    Mounts:
      /var/run/secrets/kubernetes.io/serviceaccount from default-token-xw8ld (ro)

```
kubectl describe 명령으로 컨테이너에 configMap 적용여부를 알 수 있다. 

## livenessProbe 설정
* buildspec.yaml
```
                  livenessProbe:
                    exec:
                      command:
                      - cat
                      - /tmp/a.txt
                    failureThreshold: 1
                    initialDelaySeconds: 60
                    periodSeconds: 5
                    successThreshold: 1
                    timeoutSeconds: 1
```

* livenessProbe 체크 파일 설정
```
root@labs--1864180482:/home/project# kubectl exec -it pod/prequestion-6b85bccf66-ph4cg -n skcc-ns -- /bin/sh
/ # touch /tmp/a.txt
/ # ll /tmp
/bin/sh: ll: not found
/ # ls /tmp
a.txt                                                        tomcat-docbase.1638896265197407340.8080
hsperfdata_root                                              tomcat.4301478843853089841.8080
kafka-client-jaas-config-placeholder7952035848420458531conf
``` 

* discribe 확인
```
root@labs--1864180482:/home/project# kubectl describe po pod/prequestion-6b85bccf66-ph4cg -n skcc-ns
error: there is no need to specify a resource type as a separate argument when passing arguments in resource/name form (e.g. 'kubectl get resource/<resource_name>' instead of 'kubectl get resource resource/<resource_name>'
root@labs--1864180482:/home/project# kubectl describe pod/prequestion-6b85bccf66-ph4cg -n skcc-ns
Name:           prequestion-6b85bccf66-ph4cg
Namespace:      skcc-ns
Priority:       0
Node:           ip-192-168-19-116.ec2.internal/192.168.19.116
Start Time:     Thu, 03 Sep 2020 02:49:32 +0000
Labels:         app=prequestion
                pod-template-hash=6b85bccf66
Annotations:    kubernetes.io/psp: eks.privileged
Status:         Running
IP:             192.168.25.224
IPs:            <none>
Controlled By:  ReplicaSet/prequestion-6b85bccf66
Containers:
  prequestion:
    Container ID:   docker://5b0ab394e1796e1709c34f3e9b2a8da3e91d5de967c406921985f0e2e6490108
    Image:          052937454741.dkr.ecr.us-east-1.amazonaws.com/user21-prequestion-repo:8d73499c4b4ebb0fd6686236e4c87aee8ab0891d
    Image ID:       docker-pullable://052937454741.dkr.ecr.us-east-1.amazonaws.com/user21-prequestion-repo@sha256:193f5d9f86b2d89753793327a6f5afd8d64e0a16a5633e385caa9c63fea301ac
    Port:           8080/TCP
    Host Port:      0/TCP
    State:          Running
      Started:      Thu, 03 Sep 2020 02:52:47 +0000
    Last State:     Terminated
      Reason:       Error
      Exit Code:    143
      Started:      Thu, 03 Sep 2020 02:51:42 +0000
      Finished:     Thu, 03 Sep 2020 02:52:46 +0000
    Ready:          True
    Restart Count:  3
    Liveness:       exec [cat /tmp/a.txt] delay=60s timeout=1s period=5s #success=1 #failure=1
    Readiness:      http-get http://:8080/actuator/health delay=30s timeout=2s period=5s #success=1 #failure=10
    Environment:    <none>
    Mounts:
      /var/run/secrets/kubernetes.io/serviceaccount from default-token-88cm2 (ro)
Conditions:
  Type              Status
  Initialized       True 
  Ready             True 
  ContainersReady   True 
  PodScheduled      True 
Volumes:
  default-token-88cm2:
    Type:        Secret (a volume populated by a Secret)
    SecretName:  default-token-88cm2
    Optional:    false
QoS Class:       BestEffort
Node-Selectors:  <none>
Tolerations:     node.kubernetes.io/not-ready:NoExecute for 300s
                 node.kubernetes.io/unreachable:NoExecute for 300s
Events:          <none>
```

* kubectl get all -n skcc-ns
```
pod/prequestion-6b85bccf66-ph4cg               1/1     Running   3          69m
```
