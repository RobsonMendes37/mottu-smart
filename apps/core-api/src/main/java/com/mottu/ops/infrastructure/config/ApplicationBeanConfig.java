package com.mottu.ops.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.port.in.AuthUseCase;
import com.mottu.ops.application.port.in.ManagerUserUseCase;
import com.mottu.ops.application.port.out.MotorcycleRepositoryPort;
import com.mottu.ops.application.port.out.PasswordEncoderPort;
import com.mottu.ops.application.port.out.RentalContractRepositoryPort;
import com.mottu.ops.application.port.out.TokenProviderPort;
import com.mottu.ops.application.port.out.UserRepositoryPort;
import com.mottu.ops.application.port.in.FleetManagementUseCase;
import com.mottu.ops.application.port.in.GenerateUploadUrlUseCase;
import com.mottu.ops.application.port.in.GetRiderGarageUseCase;
import com.mottu.ops.application.port.in.GetYardsUseCase;
import com.mottu.ops.application.port.out.InspectionRepositoryPort;
import com.mottu.ops.application.port.out.StoragePort;
import com.mottu.ops.application.port.out.YardRepositoryPort;
import com.mottu.ops.application.usecase.AuthService;
import com.mottu.ops.application.usecase.FleetService;
import com.mottu.ops.application.usecase.ManagerUserService;
import com.mottu.ops.application.usecase.RiderGarageService;
import com.mottu.ops.application.usecase.UploadUrlService;
import com.mottu.ops.application.usecase.YardService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationBeanConfig {

    @Bean
    public AuthUseCase authUseCase(
            UserRepositoryPort userRepositoryPort,
            PasswordEncoderPort passwordEncoderPort,
            TokenProviderPort tokenProviderPort,
            @Value("${public.demo.mode:${mottu.demo-mode:${PUBLIC_DEMO_MODE:false}}}") boolean publicDemoMode
    ) {
        return new AuthService(userRepositoryPort, passwordEncoderPort, tokenProviderPort, publicDemoMode);
    }

    @Bean
    public ManagerUserUseCase managerUserUseCase(
            UserRepositoryPort userRepositoryPort,
            MotorcycleRepositoryPort motorcycleRepositoryPort,
            RentalContractRepositoryPort rentalContractRepositoryPort
    ) {
        return new ManagerUserService(userRepositoryPort, motorcycleRepositoryPort, rentalContractRepositoryPort);
    }

    @Bean
    public GetYardsUseCase getYardsUseCase(YardRepositoryPort yardRepositoryPort) {
        return new YardService(yardRepositoryPort);
    }

    @Bean
    public GetRiderGarageUseCase getRiderGarageUseCase(
            RentalContractRepositoryPort rentalContractRepositoryPort,
            MotorcycleRepositoryPort motorcycleRepositoryPort,
            YardRepositoryPort yardRepositoryPort,
            InspectionRepositoryPort inspectionRepositoryPort
    ) {
        return new RiderGarageService(
                rentalContractRepositoryPort,
                motorcycleRepositoryPort,
                yardRepositoryPort,
                inspectionRepositoryPort
        );
    }

    @Bean
    public GenerateUploadUrlUseCase generateUploadUrlUseCase(StoragePort storagePort) {
        return new UploadUrlService(storagePort);
    }

    @Bean
    public FleetManagementUseCase fleetManagementUseCase(
            MotorcycleRepositoryPort motorcycleRepositoryPort,
            YardRepositoryPort yardRepositoryPort,
            RentalContractRepositoryPort rentalContractRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            ObjectMapper objectMapper
    ) {
        return new FleetService(
                motorcycleRepositoryPort,
                yardRepositoryPort,
                rentalContractRepositoryPort,
                userRepositoryPort,
                objectMapper
        );
    }
}

