package io.harness.springdata;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

@OwnedBy(PL)
@UtilityClass
@Slf4j
public class SpringDataMongoUtils {
  public static final FindAndModifyOptions returnNewOptions = new FindAndModifyOptions().returnNew(true).upsert(false);

  public static Update setUnset(Update ops, String field, Object value) {
    if (value == null || (value instanceof String && isBlank((String) value))) {
      return ops.unset(field);
    } else {
      return ops.set(field, value);
    }
  }

  public static Update setUnsetOnInsert(Update ops, String field, Object value) {
    if (value == null || (value instanceof String && isBlank((String) value))) {
      return ops.unset(field);
    } else {
      return ops.setOnInsert(field, value);
    }
  }

  public void populateInFilter(Criteria criteria, String fieldName, List<?> values) {
    if (isNotEmpty(values)) {
      criteria.and(fieldName).in(values);
    }
  }

  public void populateNotInFilter(Criteria criteria, String fieldName, List<?> values) {
    if (isNotEmpty(values)) {
      criteria.and(fieldName).nin(values);
    }
  }

  public void populateAllFilter(Criteria criteria, String fieldName, List<?> values) {
    if (isNotEmpty(values)) {
      criteria.and(fieldName).all(values);
    }
  }

  public static <T> Page<T> getPaginatedResult(
      Criteria criteria, Pageable pageable, Class<T> clazz, MongoTemplate mongoTemplate) {
    Query query = new Query(criteria).with(pageable);
    List<T> objects = mongoTemplate.find(query, clazz);
    return PageableExecutionUtils.getPage(
        objects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1L), clazz));
  }

  public String getPatternForMatchingAnyOneOf(List<String> wordsToBeMatched) {
    return StringUtils.collectionToDelimitedString(wordsToBeMatched, "|");
  }
}
