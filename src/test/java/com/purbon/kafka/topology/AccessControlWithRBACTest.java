package com.purbon.kafka.topology;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.roles.RBACProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class AccessControlWithRBACTest {

  @Mock RBACProvider aclsProvider;

  @Mock ClusterState clusterState;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private AccessControlManager accessControlManager;

  @Before
  public void setup() {
    accessControlManager = new AccessControlManager(aclsProvider, clusterState);
  }

  @Test
  public void testPredefinedRoles() throws IOException {

    Map<String, List<String>> predefinedRoles = new HashMap<>();
    predefinedRoles.put("ResourceOwner", Arrays.asList("User:Foo"));

    Project project = new ProjectImpl();
    project.setRbacRawRoles(predefinedRoles);

    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    TopologyAclBinding binding =
        TopologyAclBinding.build(t 
            ResourceType.TOPIC.name(),
            "foo",
            "host",
            AclOperation.DESCRIBE_CONFIGS.name(),
            "User:Foo",
            PatternType.ANY.name());
    when(aclsProvider.setPredefinedRole("User:Foo", "ResourceOwner", project.buildTopicPrefix()))
        .thenReturn(binding);

    accessControlManager.sync(topology);

    verify(aclsProvider, times(1))
        .setPredefinedRole(eq("User:Foo"), eq("ResourceOwner"), eq(project.buildTopicPrefix()));
  }
}
