package ru.itmo.soa.starship.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ru.itmo.soa.common.dto.SpaceMarineDto;
import ru.itmo.soa.common.dto.SpaceMarinesResponse;
import ru.itmo.soa.starship.exception.ErrorDescriptions;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpaceMarineClient {

    private final RestTemplate restTemplate;

    @Value("${service1.base-url:https://localhost:21038/api/v1}")
    private String baseUrl;

    public SpaceMarineDto getById(Integer id) {
        String url = baseUrl + "/space-marines/" + id;
        try {
            log.info("Requesting SpaceMarine with id={} from {}", id, url);
            return restTemplate.getForObject(url, SpaceMarineDto.class);
        } catch (HttpClientErrorException ex) {
            log.warn("Client error fetching SpaceMarine id={}: {}", id, ex.getStatusCode());
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw ErrorDescriptions.MARINE_NOT_FOUND.exception();
            } else if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw ErrorDescriptions.INVALID_MARINE_ID.exception();
            }
            throw ErrorDescriptions.SERVICE_UNAVAILABLE.exception(ex.getResponseBodyAsString());
        } catch (HttpServerErrorException ex) {
            log.error("Server error from spacemarine-service: {}", ex.getResponseBodyAsString());
            throw ErrorDescriptions.SERVICE_UNAVAILABLE.exception("Ошибка первого сервиса: " + ex.getStatusCode());
        } catch (ResourceAccessException ex) {
            log.error("Failed to connect to spacemarine-service at {}: {}", url, ex.getMessage());
            throw ErrorDescriptions.SERVICE_UNAVAILABLE.exception("Сервис космодесантников недоступен (" + url + ")");
        } catch (Exception ex) {
            log.error("Unexpected error contacting spacemarine-service", ex);
            throw ErrorDescriptions.INTERNAL_ERROR.exception();
        }
    }

    public SpaceMarinesResponse getAll() {
        String url = baseUrl + "/space-marines?pageSize=1000";
        try {
            log.info("Requesting all SpaceMarines from {}", url);
            return restTemplate.getForObject(url, SpaceMarinesResponse.class);
        } catch (HttpClientErrorException ex) {
            log.warn("Client error fetching all SpaceMarines: {}", ex.getStatusCode());
            throw ErrorDescriptions.SERVICE_UNAVAILABLE.exception(ex.getResponseBodyAsString());
        } catch (HttpServerErrorException ex) {
            log.error("Server error from spacemarine-service: {}", ex.getResponseBodyAsString());
            throw ErrorDescriptions.SERVICE_UNAVAILABLE.exception("Ошибка первого сервиса: " + ex.getStatusCode());
        } catch (ResourceAccessException ex) {
            log.error("Failed to connect to spacemarine-service at {}: {}", url, ex.getMessage());
            throw ErrorDescriptions.SERVICE_UNAVAILABLE.exception("Сервис космодесантников недоступен (" + url + ")");
        } catch (Exception ex) {
            log.error("Unexpected error contacting spacemarine-service", ex);
            throw ErrorDescriptions.INTERNAL_ERROR.exception();
        }
    }
}
