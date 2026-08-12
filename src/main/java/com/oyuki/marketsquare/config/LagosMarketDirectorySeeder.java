package com.oyuki.marketsquare.config;

import com.oyuki.marketsquare.entity.LocalGovernment;
import com.oyuki.marketsquare.entity.State;
import com.oyuki.marketsquare.repository.LocalGovernmentRepository;
import com.oyuki.marketsquare.repository.StateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LagosMarketDirectorySeeder implements CommandLineRunner {

    private static final List<String> LAGOS_LGAS = List.of(
            "Agege",
            "Ajeromi-Ifelodun",
            "Alimosho",
            "Amuwo-Odofin",
            "Apapa",
            "Badagry",
            "Epe",
            "Eti-Osa",
            "Ibeju-Lekki",
            "Ifako-Ijaiye",
            "Ikeja",
            "Ikorodu",
            "Kosofe",
            "Lagos Island",
            "Lagos Mainland",
            "Mushin",
            "Ojo",
            "Oshodi-Isolo",
            "Shomolu",
            "Surulere"
    );

    private final StateRepository stateRepository;
    private final LocalGovernmentRepository lgaRepository;

    @Override
    @Transactional
    public void run(String... args) {
        State lagos = stateRepository.findByNameIgnoreCase("Lagos")
                .orElseGet(() -> stateRepository.save(
                        State.builder().name("Lagos").active(true).build()
                ));

        for (String lgaName : LAGOS_LGAS) {
            lgaRepository.findByStateIdAndNameIgnoreCase(lagos.getId(), lgaName)
                    .orElseGet(() -> lgaRepository.save(
                            LocalGovernment.builder()
                                    .state(lagos)
                                    .name(lgaName)
                                    .active(true)
                                    .build()
                    ));
        }
    }
}
