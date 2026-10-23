package ru.itmo.soa.spacemarine.repository;

import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;
import ru.itmo.soa.spacemarine.entity.SpaceMarineEntity;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SpaceMarineRepository extends SimpleJpaRepository<SpaceMarineEntity, Integer> {

    private final EntityManager em;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private static final Map<String, String[]> SORT_FIELD_MAP = new HashMap<>();

    static {
        SORT_FIELD_MAP.put("id", new String[]{"id"});
        SORT_FIELD_MAP.put("name", new String[]{"name"});
        SORT_FIELD_MAP.put("creationDate", new String[]{"creationDate"});
        SORT_FIELD_MAP.put("coordinates.x", new String[]{"coordinates", "x"});
        SORT_FIELD_MAP.put("coordinates.y", new String[]{"coordinates", "y"});
        SORT_FIELD_MAP.put("health", new String[]{"health"});
        SORT_FIELD_MAP.put("heartCount", new String[]{"heartCount"});
        SORT_FIELD_MAP.put("weaponType", new String[]{"weaponType"});
        SORT_FIELD_MAP.put("meleeWeapon", new String[]{"meleeWeapon"});
        SORT_FIELD_MAP.put("chapter.name", new String[]{"chapter", "name"});
        SORT_FIELD_MAP.put("chapter.parentLegion", new String[]{"chapter", "parentLegion"});
        SORT_FIELD_MAP.put("chapter.world", new String[]{"chapter", "world"});
    }

    public SpaceMarineRepository(EntityManager em) {
        super(SpaceMarineEntity.class, em);
        this.em = em;
    }

    public List<SpaceMarineEntity> getAllFiltered(Map<String, String> filters,
                                                  String sortBy, String sortOrder,
                                                  int page, int pageSize) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SpaceMarineEntity> cq = cb.createQuery(SpaceMarineEntity.class);
        Root<SpaceMarineEntity> root = cq.from(SpaceMarineEntity.class);
        cq.select(root);

        List<Predicate> predicates = buildPredicates(cb, root, filters);
        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        if (sortBy != null && SORT_FIELD_MAP.containsKey(sortBy)) {
            Path<?> sortPath = resolvePath(root, SORT_FIELD_MAP.get(sortBy));
            if ("desc".equalsIgnoreCase(sortOrder)) {
                cq.orderBy(cb.desc(sortPath));
            } else {
                cq.orderBy(cb.asc(sortPath));
            }
        }

        return em.createQuery(cq)
                .setFirstResult(page * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    public long countFiltered(Map<String, String> filters) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<SpaceMarineEntity> root = cq.from(SpaceMarineEntity.class);
        cq.select(cb.count(root));

        List<Predicate> predicates = buildPredicates(cb, root, filters);
        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        return em.createQuery(cq).getSingleResult();
    }

    public Double getAverageHealth() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Double> cq = cb.createQuery(Double.class);
        Root<SpaceMarineEntity> root = cq.from(SpaceMarineEntity.class);
        cq.select(cb.avg(root.get("health")));
        Double result = em.createQuery(cq).getSingleResult();
        return result != null ? result : 0.0;
    }

    public List<SpaceMarineEntity> findByNameContaining(String substring) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SpaceMarineEntity> cq = cb.createQuery(SpaceMarineEntity.class);
        Root<SpaceMarineEntity> root = cq.from(SpaceMarineEntity.class);
        cq.where(cb.like(cb.lower(root.get("name")), "%" + substring.toLowerCase() + "%"));
        return em.createQuery(cq).getResultList();
    }

    public List<SpaceMarineEntity> findByHealthGreaterThan(Double threshold) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SpaceMarineEntity> cq = cb.createQuery(SpaceMarineEntity.class);
        Root<SpaceMarineEntity> root = cq.from(SpaceMarineEntity.class);
        cq.where(cb.greaterThan(root.get("health"), threshold));
        return em.createQuery(cq).getResultList();
    }

    public boolean isValidSortField(String sortBy) {
        return sortBy == null || SORT_FIELD_MAP.containsKey(sortBy);
    }

    private Path<?> resolvePath(Root<SpaceMarineEntity> root, String[] parts) {
        Path<?> path = root;
        for (String part : parts) {
            path = path.get(part);
        }
        return path;
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb,
                                             Root<SpaceMarineEntity> root,
                                             Map<String, String> filters) {
        List<Predicate> predicates = new ArrayList<>();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.isEmpty()) continue;

            try {
                switch (key) {
                    case "id":
                        predicates.add(cb.equal(root.get("id"), Integer.parseInt(value)));
                        break;
                    case "name":
                        predicates.add(cb.equal(root.get("name"), value));
                        break;
                    case "coordinates.x":
                        predicates.add(cb.equal(root.get("coordinates").get("x"), Integer.parseInt(value)));
                        break;
                    case "coordinates.y":
                        predicates.add(cb.equal(root.get("coordinates").get("y"), Integer.parseInt(value)));
                        break;
                    case "creationDate":
                        predicates.add(cb.equal(root.get("creationDate"), LocalDateTime.parse(value, DATE_FMT)));
                        break;
                    case "health":
                        predicates.add(cb.equal(root.get("health"), Double.parseDouble(value)));
                        break;
                    case "heartCount":
                        predicates.add(cb.equal(root.get("heartCount"), Integer.parseInt(value)));
                        break;
                    case "weaponType":
                        predicates.add(cb.equal(root.get("weaponType"),
                                ru.itmo.soa.common.enums.Weapon.valueOf(value)));
                        break;
                    case "meleeWeapon":
                        predicates.add(cb.equal(root.get("meleeWeapon"),
                                ru.itmo.soa.common.enums.MeleeWeapon.valueOf(value)));
                        break;
                    case "chapter.name":
                        predicates.add(cb.equal(root.get("chapter").get("name"), value));
                        break;
                    case "chapter.parentLegion":
                        predicates.add(cb.equal(root.get("chapter").get("parentLegion"), value));
                        break;
                    case "chapter.world":
                        predicates.add(cb.equal(root.get("chapter").get("world"), value));
                        break;
                    default:
                        break;
                }
            } catch (IllegalArgumentException | java.time.DateTimeException e) {
                throw new ru.itmo.soa.spacemarine.exception.ValidationException(
                        422, "Некорректное значение фильтра для поля " + key + ": " + value);
            }
        }
        return predicates;
    }
}
