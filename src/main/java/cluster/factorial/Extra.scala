package cluster.factorial

import akka.actor.{Actor, Props}
import akka.cluster.routing.{HeapMetricsSelector, SystemLoadAverageMetricsSelector, AdaptiveLoadBalancingPool, AdaptiveLoadBalancingGroup}

// not used, only for documentation
abstract class FactorialFrontend2 extends Actor {
  //#router-lookup-in-code
  import akka.cluster.routing.{ClusterRouterGroup, ClusterRouterGroupSettings}

  val backend = context.actorOf(
    ClusterRouterGroup(AdaptiveLoadBalancingGroup(HeapMetricsSelector),
      ClusterRouterGroupSettings(
        totalInstances = 100, routeesPaths = List("/user/factorialBackend"),
        allowLocalRoutees = true, useRole = Some("backend"))).props(),
    name = "factorialBackendRouter2")
  //#router-lookup-in-code
}

// not used, only for documentation
abstract class FactorialFrontend3 extends Actor {
  //#router-deploy-in-code
  import akka.cluster.routing.{ClusterRouterPool, ClusterRouterPoolSettings}

  val backend = context.actorOf(
    ClusterRouterPool(AdaptiveLoadBalancingPool(
      SystemLoadAverageMetricsSelector), ClusterRouterPoolSettings(
      totalInstances = 100, maxInstancesPerNode = 3,
      allowLocalRoutees = false, useRole = Some("backend"))).props(Props[FactorialBackend]),
    name = "factorialBackendRouter3")
  //#router-deploy-in-code
}