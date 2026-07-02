package com.framecut.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sync_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncState {
    @Id
    private Long id;
    private int moviePage;
    private int tvPage;
}
