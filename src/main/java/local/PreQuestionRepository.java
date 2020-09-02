package local;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface PreQuestionRepository extends PagingAndSortingRepository<PreQuestion, Long>{
    List<PreQuestion> findByHospitalId(String HospitalId);
    PreQuestion findByScreeningId(Long ScreeningId);
}