package com.yiwilee.aiqasystem.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@Entity
@Table(name = "sys_permission")
@NoArgsConstructor
@AllArgsConstructor
public class SysPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId = 0L;

    @Column(name = "perm_name", nullable = false)
    private String permName;

    @Column(name = "perm_code")
    private String permCode;

    private Integer type;

    private String path;
}