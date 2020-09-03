
package local.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name="HospitalManage", url="${api.hospital.url}")//,fallback = HospitalServiceFallback.class)
public interface HospitalService {

    @RequestMapping(method= RequestMethod.PUT, value="/hospitals/{hospitalId}", consumes = "application/json")
    public void screeningRequest(@PathVariable("hospitalId") Long hospitalId, @RequestBody Hospital hospital);

}