package com.mrkirby153.bfs.sql;

import com.mrkirby153.bfs.sql.elements.JoinElement.Type;
import org.junit.Assert;
import org.junit.Test;

public class QueryBuilderMySQLTest {

    @Test
    public void testTable() {
        QueryBuilder queryBuilder = new QueryBuilder().table("testing");
        String sql = queryBuilder.toSql();
        Assert.assertEquals("testing", queryBuilder.getTable());
        Assert.assertEquals("SELECT * FROM `testing`", sql);

        queryBuilder = new QueryBuilder().from("test");
        Assert.assertEquals("test", queryBuilder.getTable());
    }

    @Test
    public void testWhere() {
        QueryBuilder where = new QueryBuilder().table("testing").where("test", "=", "blue");
        String sql = where.toSql();
        Assert.assertNotEquals(0, where.getWheres().size());
        Assert.assertEquals("SELECT * FROM `testing` WHERE `test` = ?", sql);
    }

    @Test
    public void testMultipleWheres() {
        QueryBuilder builder = new QueryBuilder().table("testing").where("one", "=", "two")
            .where("pigs", ">", "fly");
        Assert.assertEquals(2, builder.getWheres().size());
        Assert.assertEquals("SELECT * FROM `testing` WHERE `one` = ? AND `pigs` > ?",
            builder.toSql());
    }

    @Test
    public void testWhereDefault() {
        QueryBuilder builder = new QueryBuilder().table("testing").where("pigs", "fly");
        Assert.assertEquals(1, builder.getWheres().size());
        Assert.assertEquals("SELECT * FROM `testing` WHERE `pigs` = ?", builder.toSql());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidWhere() {
        new QueryBuilder().where("test", "??", "testing");
    }

    @Test
    public void testOrderBy() {
        QueryBuilder builder = new QueryBuilder().table("testing").orderBy("test", "ASC");
        String sql = builder.toSql();
        Assert.assertNotEquals(0, builder.getOrders().size());
        Assert.assertEquals("SELECT * FROM `testing` ORDER BY `test` ASC", sql);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOrderByInvalid() {
        QueryBuilder builder = new QueryBuilder().table("testing").orderBy("test", "$$$");
    }

    @Test
    public void testOrderByMultiple() {
        QueryBuilder builder = new QueryBuilder().table("testing").orderBy("test", "ASC")
            .orderBy("one", "DESC");
        Assert.assertEquals(2, builder.getOrders().size());
        Assert.assertEquals("SELECT * FROM `testing` ORDER BY `test` ASC, `one` DESC",
            builder.toSql());
    }

    @Test
    public void testSelect() {
        QueryBuilder builder = new QueryBuilder().table("testing").select("one", "two");
        Assert.assertEquals(2, builder.getColumns().length);
        Assert.assertEquals("SELECT `one`, `two` FROM `testing`", builder.toSql());
    }

    @Test
    public void testSelectTable(){
        QueryBuilder builder = new QueryBuilder().table("testing").select("table.test");
        Assert.assertEquals(1, builder.getColumns().length);
        Assert.assertEquals("SELECT `table`.`test` FROM `testing`", builder.toSql());
    }

    @Test
    public void testWithWildcard(){
        QueryBuilder builder = new QueryBuilder().table("testing").select("table.*");
        Assert.assertEquals("SELECT `table`.* FROM `testing`", builder.toSql());
    }

    @Test
    public void testLimit() {
        QueryBuilder builder = new QueryBuilder();
        Assert.assertNull(builder.getLimit());
        builder.limit(10);
        Assert.assertEquals(Long.valueOf(10), builder.getLimit());
        builder.table("test");
        Assert.assertEquals("SELECT * FROM `test` LIMIT 10", builder.toSql());
    }

    @Test
    public void testOffset() {
        QueryBuilder builder = new QueryBuilder().table("test");
        Assert.assertNull(builder.getOffset());
        builder.offset(5);
        Assert.assertEquals(Long.valueOf(5), builder.getOffset());
        Assert.assertEquals("SELECT * FROM `test` OFFSET 5", builder.toSql());
    }

    @Test
    public void testDistinct() {
        QueryBuilder builder = new QueryBuilder().table("test");
        Assert.assertFalse(builder.isDistinct());
        builder.distinct();
        Assert.assertTrue(builder.isDistinct());
        Assert.assertEquals("SELECT DISTINCT * FROM `test`", builder.toSql());
    }

    @Test
    public void testJoin() {
        QueryBuilder builder = new QueryBuilder().table("test")
            .join(Type.INNER, "testing", "id", "=", "test");
        Assert
            .assertEquals("SELECT * FROM `test` INNER JOIN testing ON id = test", builder.toSql());
    }

    @Test
    public void testLeftJoin() {
        QueryBuilder builder = new QueryBuilder().table("test")
            .leftJoin("testing", "id", "=", "test");
        Assert.assertEquals("SELECT * FROM `test` LEFT JOIN testing ON id = test", builder.toSql());
    }

    @Test
    public void testRightJoin() {
        QueryBuilder builder = new QueryBuilder().table("test")
            .rightJoin("testing", "id", "=", "test");
        Assert
            .assertEquals("SELECT * FROM `test` RIGHT JOIN testing ON id = test", builder.toSql());
    }

    @Test
    public void testOuterJoin() {
        QueryBuilder builder = new QueryBuilder().table("test")
            .outerJoin("testing", "id", "=", "test");
        Assert.assertEquals("SELECT * FROM `test` FULL OUTER JOIN testing ON id = test",
            builder.toSql());
    }

    @Test
    public void testInnerJoin() {
        QueryBuilder builder = new QueryBuilder().table("test")
            .innerJoin("testing", "id", "=", "test");
        Assert
            .assertEquals("SELECT * FROM `test` INNER JOIN testing ON id = test", builder.toSql());
    }
}