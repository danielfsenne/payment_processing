package com.paymentprocessing.payment_service.client;

import java.util.UUID;

public record CustomerDto(UUID id, String name, String email, String document) {
}
