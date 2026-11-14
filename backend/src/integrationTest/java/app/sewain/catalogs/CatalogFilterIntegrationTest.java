package app.sewain.catalogs;

import app.sewain.shared.PageQuery;
import app.sewain.testsupport.JpaIntegrationSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies actual query results, including negative multi-valued rules and wildcard escaping. */
class CatalogFilterIntegrationTest extends JpaIntegrationSupport {
    CatalogFixture tent,stove,bag;
    @BeforeEach void seedCatalog() {
        tent=catalogFixture("Alpine Tent","AVAILABLE");stove=catalogFixture("Cooking Stove","MAINTENANCE");bag=catalogFixture("Tent Bag","AVAILABLE");
    }
    Collection collection(CollectionMatchCondition condition,CollectionRule... rules) {
        var c=new Collection();c.setMatchCondition(condition);c.getRules().addAll(List.of(rules));return c;
    }
    CollectionRule rule(CollectionRuleAttribute attribute,CollectionRuleOperator operator,String... values) {
        var r=new CollectionRule();r.setAttribute(attribute);r.setOperator(operator);for(var value:values)r.addValue(value);return r;
    }
    List<UUID> matching(Collection c) {
        return matching(CollectionRuleSpecifications.matching(c));
    }
    List<UUID> matching(Specification<Product> specification) {
        return inTransaction(()->context.getBean(ProductRepository.class).findAll(specification).stream().map(Product::getId).toList());
    }
    void tag(String name,CatalogFixture... fixtures) {
        var id=UUID.randomUUID();jdbc.update("INSERT INTO "+tenant+".tags(id,name,slug) VALUES (?,?,?)",id,name,name.toLowerCase());
        for(var f:fixtures)jdbc.update("INSERT INTO "+tenant+".product_tags(product_id,tag_id) VALUES (?,?)",f.productId(),id);
    }
    void products(Collection c,UUID... expected) {
        var actual=matching(c);assertEquals(Set.of(expected),new HashSet<>(actual));assertEquals(expected.length,actual.size(),"Joins must not duplicate products");
    }
    @Test void titleOperatorsHaveCaseInsensitiveLiteralSemantics() {
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.TITLE,CollectionRuleOperator.EQUALS," ALPINE TENT ")),tent.productId());
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.TITLE,CollectionRuleOperator.CONTAINS,"TeNt")),tent.productId(),bag.productId());
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.TITLE,CollectionRuleOperator.NOT_CONTAINS,"tent")),stove.productId());
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.TITLE,CollectionRuleOperator.NOT_EQUALS,"Alpine Tent")),stove.productId(),bag.productId());
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.TITLE,CollectionRuleOperator.STARTS_WITH,"alpine")),tent.productId());
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.TITLE,CollectionRuleOperator.ENDS_WITH,"TENT")),tent.productId());
    }
    @Test void wildcardCharactersAreMatchedLiterally() {
        jdbc.update("UPDATE "+tenant+".products SET name=? WHERE id=?","Tent %_! literal",tent.productId());
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.TITLE,CollectionRuleOperator.CONTAINS,"%_!")),tent.productId());
    }
    @Test void negativeTagRuleMeansNoneOfTheValuesMatch() {
        tag("Camping",tent,stove);tag("Hiking",tent);
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.TAG,CollectionRuleOperator.NOT_EQUALS,"Camping","Hiking")),bag.productId());
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.TAG,CollectionRuleOperator.CONTAINS,"ing")),tent.productId(),stove.productId());
    }
    @Test void valuesWithinRuleAreOrWhileAllAndAnyControlBetweenRules() {
        tag("Camping",tent,stove);
        var title=rule(CollectionRuleAttribute.TITLE,CollectionRuleOperator.CONTAINS,"tent");var tag=rule(CollectionRuleAttribute.TAG,CollectionRuleOperator.EQUALS,"camping");
        products(collection(CollectionMatchCondition.ALL,title,tag),tent.productId());
        products(collection(CollectionMatchCondition.ANY,title,tag),tent.productId(),stove.productId(),bag.productId());
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.TITLE,CollectionRuleOperator.EQUALS,"Alpine Tent","Cooking Stove")),tent.productId(),stove.productId());
    }
    @Test void productsWithoutTypeMatchNegativeTypeRule() {
        var id=UUID.randomUUID();jdbc.update("INSERT INTO "+tenant+".product_types(id,name,slug) VALUES (?,?,'camping')",id,"Camping");
        jdbc.update("UPDATE "+tenant+".products SET product_type_id=? WHERE id=?",id,tent.productId());
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.TYPE,CollectionRuleOperator.EQUALS,"CAMPING")),tent.productId());
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.TYPE,CollectionRuleOperator.NOT_EQUALS,"Camping")),stove.productId(),bag.productId());
    }
    @Test void variantTitleAndAttributeRulesCorrelateToTheCorrectProduct() {
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.VARIANT_TITLE,CollectionRuleOperator.CONTAINS,"stove")),stove.productId());
        var attribute=UUID.randomUUID();var value=UUID.randomUUID();
        jdbc.update("INSERT INTO "+tenant+".attributes(id,name,slug) VALUES (?,'Capacity','capacity')",attribute);
        jdbc.update("INSERT INTO "+tenant+".attribute_values(id,attribute_id,value,slug) VALUES (?,?,'Large','large')",value,attribute);
        jdbc.update("INSERT INTO "+tenant+".product_variant_values(variant_id,attribute_id,attribute_value_id) VALUES (?,?,?)",tent.variantId(),attribute,value);
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.ATTRIBUTE_VALUE,CollectionRuleOperator.EQUALS,"large")),tent.productId());
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.ATTRIBUTE_VALUE,CollectionRuleOperator.NOT_EQUALS,"large")),stove.productId(),bag.productId());
    }
    @Test void noRulesMatchNothingAndSpecificationRetainsItsSnapshot() {
        products(collection(CollectionMatchCondition.ALL));
        products(collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.TITLE,CollectionRuleOperator.EQUALS)));
        var c=collection(CollectionMatchCondition.ALL,rule(CollectionRuleAttribute.TITLE,CollectionRuleOperator.EQUALS,"Alpine Tent"));
        var snapshot=CollectionRuleSpecifications.matching(c);c.getRules().clear();assertEquals(List.of(tent.productId()),matching(snapshot));
    }
    @Test void inventoryCombinesTextFieldsWithOrAndStatusWithAnd() {
        var repository=context.getBean(InventoryUnitRepository.class);
        var spec=InventorySpecifications.search(Map.of("variant.product.name","Alpine","skuBarcode","UNIT-"+stove.unitId(),"status","AVAILABLE"));
        var ids=inTransaction(()->repository.findAll(spec).stream().map(InventoryUnit::getId).toList());assertEquals(List.of(tent.unitId()),ids);
        var textOnly=InventorySpecifications.search(Map.of("variant.product.name","Alpine","skuBarcode","UNIT-"+stove.unitId()));
        assertEquals(Set.of(tent.unitId(),stove.unitId()),new HashSet<>(inTransaction(()->repository.findAll(textOnly).stream().map(InventoryUnit::getId).toList())));
    }
    @Test void inventoryIgnoresUnknownSortOnlyAndEmptySearchFields() {
        var repository=context.getBean(InventoryUnitRepository.class);
        var filters=new HashMap<String,String>();filters.put("unknown","ignored");filters.put("createdAt","invalid-date");filters.put("skuBarcode"," ");filters.put("serialNumber",null);
        assertEquals(3,inTransaction(()->repository.findAll(InventorySpecifications.search(filters)).size()));
        assertEquals(3,inTransaction(()->repository.findAll(InventorySpecifications.search(null)).size()));
        assertEquals(3,inTransaction(()->repository.findAll(InventorySpecifications.search(Map.of())).size()));
        assertEquals(2,inTransaction(()->repository.findAll(InventorySpecifications.search(Map.of("status","AVAILABLE"))).size()));
    }
    @Test void inventorySearchTreatsPercentAndUnderscoreAsLiteral() {
        jdbc.update("UPDATE "+tenant+".inventory_units SET serial_number='SPECIAL_%!' WHERE id=?",tent.unitId());
        var repository=context.getBean(InventoryUnitRepository.class);
        assertEquals(List.of(tent.unitId()),inTransaction(()->repository.findAll(InventorySpecifications.search(Map.of("serialNumber","_%!"))).stream().map(InventoryUnit::getId).toList()));
    }
}
