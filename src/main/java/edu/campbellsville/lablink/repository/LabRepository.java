package edu.campbellsville.lablink.repository;

import edu.campbellsville.lablink.model.Lab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabRepository extends JpaRepository<Lab, Long> {

    List<Lab> findAllByOrderByNameAsc();
}
