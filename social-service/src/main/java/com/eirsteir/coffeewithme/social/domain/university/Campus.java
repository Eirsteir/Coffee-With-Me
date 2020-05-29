package com.eirsteir.coffeewithme.social.domain.university;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
public class Campus {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private String name;

  @ManyToOne
  @JoinColumn(name = "university_id")
  @ToString.Exclude
  @JsonIgnore
  private University university;

}
