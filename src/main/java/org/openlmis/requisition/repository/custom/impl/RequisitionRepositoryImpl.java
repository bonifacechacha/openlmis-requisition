package org.openlmis.requisition.repository.custom.impl;

import org.openlmis.requisition.domain.Requisition;
import org.openlmis.requisition.domain.RequisitionStatus;
import org.openlmis.requisition.repository.custom.RequisitionRepositoryCustom;
import org.openlmis.utils.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public class RequisitionRepositoryImpl implements RequisitionRepositoryCustom {

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Method returns all Requisitions with matched parameters.
   *
   * @param facility            Facility of searched Requisitions.
   * @param program             Program of searched Requisitions.
   * @param createdDateFrom     After what date should searched Requisition be created.
   * @param createdDateTo       Before what date should searched Requisition be created.
   * @param processingPeriod    ProcessingPeriod of searched Requisitions.
   * @param supervisoryNode     SupervisoryNode of searched Requisitions.
   * @param requisitionStatuses Statuses of searched Requisitions.
   * @return List of Requisitions with matched parameters.
   */
  public Page<Requisition> searchRequisitions(UUID facility, UUID program,
                                              LocalDateTime createdDateFrom,
                                              LocalDateTime createdDateTo,
                                              UUID processingPeriod,
                                              UUID supervisoryNode,
                                              RequisitionStatus[] requisitionStatuses,
                                              Boolean emergency,
                                              Pageable pageable) {
    //Retrieve a paginated set of results
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<Requisition> queryMain = builder.createQuery(Requisition.class);
    Root<Requisition> root = queryMain.from(Requisition.class);

    Predicate predicate = builder.conjunction();
    if (facility != null) {
      predicate = builder.and(predicate, builder.equal(root.get("facilityId"), facility));
    }
    if (program != null) {
      predicate = builder.and(predicate, builder.equal(root.get("programId"), program));
    }
    if (createdDateFrom != null) {
      predicate = builder.and(predicate,
          builder.greaterThanOrEqualTo(root.get("createdDate"), createdDateFrom));
    }
    if (createdDateTo != null) {
      predicate = builder.and(predicate,
          builder.lessThanOrEqualTo(root.get("createdDate"), createdDateTo));
    }
    if (processingPeriod != null) {
      predicate = builder.and(predicate,
          builder.equal(root.get("processingPeriodId"), processingPeriod));
    }
    if (supervisoryNode != null) {
      predicate = builder.and(predicate,
          builder.equal(root.get("supervisoryNodeId"), supervisoryNode));
    }
    predicate = filterByStatuses(builder, predicate, requisitionStatuses, root);
    if (null != emergency) {
      predicate = builder.and(predicate,
          builder.equal(root.get("emergency"), emergency));
    }

    queryMain.where(predicate);

    int pageNumber = Pagination.getPageNumber(pageable);
    int pageSize = Pagination.getPageSize(pageable);

    List<Requisition> results = entityManager.createQuery(queryMain)
                                .setFirstResult(pageNumber * pageSize)
                                .setMaxResults(pageSize)
                                .getResultList();

    //Having retrieved just paginated values we care about, determine
    //the total number of values in the system which meet our criteria.
    CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);
    Root<Requisition> rootQueryCount = queryCount.from(Requisition.class);
    queryCount.select(builder.count(rootQueryCount));
    queryCount.where(predicate);
    Long count = entityManager.createQuery(queryCount).getSingleResult();

    return Pagination.getPage(results, pageable, count);
  }


  /**
   * Method returns all Requisitions with matched parameters.
   *
   * @param processingPeriod ProcessingPeriod of searched Requisitions.
   * @param emergency        if {@code true}, the method will look only for emergency requisitions,
   *                         if {@code false}, the method will look only for standard requisitions,
   *                         if {@code null} the method will check all requisitions.
   * @return List of Requisitions with matched parameters.
   */
  public List<Requisition> searchByProcessingPeriodAndType(UUID processingPeriod,
                                                           Boolean emergency) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Requisition> query = builder.createQuery(Requisition.class);
    Root<Requisition> root = query.from(Requisition.class);
    Predicate predicate = builder.conjunction();

    if (null != emergency) {
      predicate = builder.and(predicate, builder.equal(root.get("emergency"), emergency));
    }

    if (processingPeriod != null) {
      predicate = builder.and(predicate,
          builder.equal(root.get("processingPeriodId"), processingPeriod));
    }
    query.where(predicate);
    return entityManager.createQuery(query).getResultList();
  }

  /**
   * Get approved requisitions matching all of provided parameters.
   *
   * @param filterBy     Field used to filter: "programName","facilityCode","facilityName" or
   *                     "all".
   * @param desiredUuids Desired UUID list.
   * @param pageable     Pageable object that allows to optionally add "page" (page number)
   *                     and "size" (page size) query parameters.
   * @return List of requisitions.
   */
  @Override
  public Page<Requisition> searchApprovedRequisitions(String filterBy, List<UUID> desiredUuids,
                                                      Pageable pageable) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Requisition> criteriaQuery = builder.createQuery(Requisition.class);

    Root<Requisition> root = criteriaQuery.from(Requisition.class);

    Path<UUID> facility = root.get("facilityId");
    Path<UUID> program = root.get("programId");

    Predicate predicate =
        setFiltering(filterBy, builder, root, facility, program, desiredUuids);

    int pageNumber = Pagination.getPageNumber(pageable);
    int pageSize = Pagination.getPageSize(pageable);

    criteriaQuery = criteriaQuery.where(predicate);
    List<Requisition> results = entityManager.createQuery(criteriaQuery)
                                      .setFirstResult(pageNumber * pageSize)
                                      .setMaxResults(pageSize)
                                      .getResultList();

    //Having retrieved just paginated values we care about, determine
    //the total number of values in the system which meet our criteria.
    CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);
    Root<Requisition> rootQueryCount = queryCount.from(Requisition.class);
    queryCount.select(builder.count(rootQueryCount));
    queryCount.where(predicate);
    Long count = entityManager.createQuery(queryCount).getSingleResult();

    return Pagination.getPage(results, pageable, count);
  }

  /**
   * Get last regular requisition for the given facility and program.
   *
   * @param facility UUID of facility.
   * @param program  UUID of program.
   * @return last regular requisition for the given facility and program. {@code null} if not found.
   * @throws IllegalArgumentException if any of arguments is {@code null}.
   */
  @Override
  public Requisition getLastRegularRequisition(UUID facility, UUID program) {
    if (null == facility || null == program) {
      throw new IllegalArgumentException("facility and program must be provided");
    }

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<Requisition> query = builder.createQuery(Requisition.class);

    Root<Requisition> root = query.from(Requisition.class);

    Predicate predicate = builder.conjunction();
    predicate = builder.and(predicate, builder.equal(root.get("emergency"), false));
    predicate = builder.and(predicate, builder.equal(root.get("facilityId"), facility));
    predicate = builder.and(predicate, builder.equal(root.get("programId"), program));

    query.where(predicate);
    query.orderBy(builder.desc(root.get("createdDate")));

    List<Requisition> list = entityManager.createQuery(query).setMaxResults(1).getResultList();
    return null == list || list.isEmpty() ? null : list.get(0);
  }

  private Predicate setFiltering(String filterBy, CriteriaBuilder builder, Root<Requisition> root,
                                 Path<UUID> facility, Path<UUID> program, List<UUID> desiredUuids) {

    //Add first important filter
    Predicate predicate = builder.equal(root.get("status"), RequisitionStatus.APPROVED);

    if (filterBy != null && !filterBy.isEmpty()) {
      //Add second important filter
      Predicate predicateFilterBy = builder.disjunction();

      if (!desiredUuids.isEmpty()) {
        predicateFilterBy = builder.or(predicateFilterBy, facility.in(desiredUuids));
        predicateFilterBy = builder.or(predicateFilterBy, program.in(desiredUuids));
      }
      //Connector filters
      predicate = builder.and(predicate, predicateFilterBy);
    }

    return predicate;
  }

  private Predicate filterByStatuses(CriteriaBuilder builder, Predicate predicate,
                                     RequisitionStatus[] requisitionStatuses,
                                     Root<Requisition> root) {

    Predicate predicateToUse = predicate;

    if (requisitionStatuses != null && requisitionStatuses.length > 0) {
      Predicate statusPredicate = builder.disjunction();
      for (RequisitionStatus status : requisitionStatuses) {
        statusPredicate = builder.or(statusPredicate,
            builder.equal(root.get("status"), status));
      }
      predicateToUse = builder.and(predicate, statusPredicate);
    }

    return predicateToUse;
  }
}