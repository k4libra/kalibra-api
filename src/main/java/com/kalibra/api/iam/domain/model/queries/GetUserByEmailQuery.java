package com.kalibra.api.iam.domain.model.queries;

import com.kalibra.api.iam.domain.model.valueobjects.Email;

public record GetUserByEmailQuery(Email email) { }
