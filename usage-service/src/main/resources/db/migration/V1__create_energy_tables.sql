CREATE TABLE hourly_usage (
    hour TIMESTAMP NOT NULL,
    community_produced DOUBLE PRECISION NOT NULL,
    community_used DOUBLE PRECISION NOT NULL,
    grid_used DOUBLE PRECISION NOT NULL,
    CONSTRAINT pk_hourly_usage PRIMARY KEY (hour)
);

CREATE TABLE current_percentage (
    hour TIMESTAMP NOT NULL,
    community_depleted DOUBLE PRECISION NOT NULL,
    grid_portion DOUBLE PRECISION NOT NULL,
    CONSTRAINT pk_current_percentage PRIMARY KEY (hour)
);
