package local;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CqrsRepository extends CrudRepository<CqrsEvt, Long> {

    List<CqrsEvt> findByScreeningId(Long screeningId);

}