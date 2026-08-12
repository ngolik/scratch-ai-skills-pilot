package com.example.scratch.greeting;

import java.util.List;

public record ValidationErrorResponse(String error, List<FieldErrorDetail> details) {
}
