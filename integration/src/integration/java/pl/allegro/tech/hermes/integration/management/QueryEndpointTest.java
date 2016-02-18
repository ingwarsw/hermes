package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;

import java.util.List;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class QueryEndpointTest extends IntegrationTest {

    @DataProvider(name = "groupData")
    public static Object[][] groupData() {
        return new Object[][] {
                {4, "{\"query\": {}}"},
                {1, "{\"query\": {\"groupName\": \"testGroup1\"}}"},
                {1, "{\"query\": {\"groupName\": {\"like\": \".*Group2\"}}}"},
                {3, "{\"query\": {\"groupName\": {\"like\": \".*Group.*\"}}}"},
                {1, "{\"query\": {\"technicalOwner\": \"Owner2\", \"supportTeam\": \"Support3\"}}"},
                {1, "{\"query\": {\"and\": [{\"technicalOwner\": \"Owner2\"}, {\"supportTeam\": \"Support3\"}]}}"},
                {3, "{\"query\": {\"or\": [{\"technicalOwner\": \"Owner2\"}, {\"supportTeam\": \"Support3\"}]}}"},
        };
    }

    @Test(dataProvider = "groupData")
    public void shouldQueryGroup(Integer resultSize, String query) {
        // given
        management.group().create(new Group("testGroup1", "Owner1", "Support3", "Contact1"));
        management.group().create(new Group("testNot1", "Owner2", "Support3", "Contact1"));
        management.group().create(new Group("testGroup2", "Owner2", "Support4", "Contact2"));
        management.group().create(new Group("testGroup3", "Owner1", "Support2", "Contact1"));

        // when
        List<Group> found = management.query().queryGroup(query);

        // then
        assertThat(found).hasSize(resultSize);
    }

    @DataProvider(name = "topicData")
    public static Object[][] topicData() {
        return new Object[][] {
                {4, "{\"query\": {}}"},
                {1, "{\"query\": {\"name\": \"testGroup1.testTopic1\"}}"},
                {1, "{\"query\": {\"name\": {\"like\": \".*testTopic1\"}}}"},
                {3, "{\"query\": {\"name\": {\"like\": \".*testTopic.*\"}}}"},
                {1, "{\"query\": {\"trackingEnabled\": \"true\", \"contentType\": \"AVRO\"}}"},
                {1, "{\"query\": {\"and\": [{\"trackingEnabled\": \"true\"}, {\"contentType\": \"AVRO\"}]}}"},
                {3, "{\"query\": {\"or\": [{\"trackingEnabled\": \"true\"}, {\"contentType\": \"AVRO\"}]}}"},
        };
    }

    @Test(dataProvider = "topicData")
    public void shouldQueryTopic(Integer resultSize, String query) {
        // given
        operations.buildTopic(topic().withName("testGroup1", "testTopic1").withContentType(AVRO).withTrackingEnabled(false).build());
        operations.buildTopic(topic().withName("testGroup1", "testTopic2").withContentType(JSON).withTrackingEnabled(false).build());
        operations.buildTopic(topic().withName("testGroup1", "testTopic3").withContentType(AVRO).withTrackingEnabled(true).build());
        operations.buildTopic(topic().withName("testGroup2", "testOtherTopic").withContentType(JSON).withTrackingEnabled(true).build());

        // when
        List<Topic> found = management.query().queryTopic(query);

        // then
        assertThat(found).hasSize(resultSize);
    }

    @DataProvider(name = "subscriptionData")
    public static Object[][] subscriptionData() {
        return new Object[][] {
                {4, "{\"query\": {}}"},
                {1, "{\"query\": {\"name\": \"subscription1\"}}"},
                {1, "{\"query\": {\"name\": {\"like\": \".*cription1\"}}}"},
                {3, "{\"query\": {\"name\": {\"like\": \"subscript.*\"}}}"},
                {1, "{\"query\": {\"name\": \"subscription1\", \"endpoint\": \"http://endpoint1\"}}"},
                {1, "{\"query\": {\"and\": [{\"name\": \"subscription1\"}, {\"endpoint\": \"http://endpoint1\"}]}}"},
                {2, "{\"query\": {\"or\": [{\"name\": \"subscription1\"}, {\"endpoint\": \"http://endpoint1\"}]}}"},
        };
    }

    @Test(dataProvider = "subscriptionData")
    public void shouldQuerySubscription(Integer resultSize, String query) {
        // given
        Topic topic = operations.buildTopic("testGroup1", "testTopic1");
        operations.createSubscription(topic, enrichSubscription(subscription().withName("subscription1"), "http://endpoint1"));
        operations.createSubscription(topic, enrichSubscription(subscription().withName("subscription2"), "http://endpoint2"));
        operations.createSubscription(topic, enrichSubscription(subscription().withName("subTestScription3"), "http://endpoint1"));
        operations.createSubscription(topic, enrichSubscription(subscription().withName("subscription4"), "http://endpoint2"));

        // when
        List<Subscription> found = management.query().querySubscription(query);

        // then
        assertThat(found).hasSize(resultSize);
    }

    private Subscription enrichSubscription(Subscription.Builder subscription, String endpoint) {
        return subscription
                .withTrackingEnabled(true)
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults().build())
                .withSupportTeam("team")
                .withEndpoint(EndpointAddress.of(endpoint))
                .build();
    }
}
