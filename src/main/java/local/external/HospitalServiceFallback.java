
package local.external;

import org.springframework.stereotype.Component;

@Component
public class HospitalServiceFallback implements HospitalService {

    @Override
    public void screeningRequest(Long hospitalId, Hospital hospital) {
        System.out.println("Circuit breaker has been opened. Fallback returned instead.");
    }
}