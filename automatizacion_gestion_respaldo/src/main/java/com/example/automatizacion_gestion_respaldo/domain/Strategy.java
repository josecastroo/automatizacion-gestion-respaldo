package com.example.automatizacion_gestion_respaldo.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "strategy")
@Getter
@Setter
public class Strategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "database_target_id")
    private DatabaseTarget databaseTarget;

    @Column(nullable = false)
    private Boolean active = true;

    @NotNull @Min(1)
    @Column(nullable = false)
    private Integer retentionDays;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String responsible;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_\\-./\\\\: ]{1,255}$")
    @Column(length = 255)
    private String destinationPath;


    @OneToMany(mappedBy = "strategy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StrategyComponent> components = new ArrayList<>();

    @OneToMany(mappedBy = "strategy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StrategyBackupType> backupTypes = new ArrayList<>();

    @OneToMany(mappedBy = "strategy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Schedule> schedules = new ArrayList<>();

    public void addComponent(StrategyComponent c) { components.add(c); c.setStrategy(this); }
    public void addBackupType(StrategyBackupType b) { backupTypes.add(b); b.setStrategy(this); }
    public void addSchedule(Schedule s) { schedules.add(s); s.setStrategy(this); }
}