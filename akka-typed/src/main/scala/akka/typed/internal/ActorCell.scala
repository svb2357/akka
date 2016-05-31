/**
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com/>
 */
package akka.typed
package internal

import akka.actor.InvalidActorNameException
import akka.util.Helpers
import scala.concurrent.duration.{ Duration, FiniteDuration }
import akka.dispatch.ExecutionContexts
import scala.concurrent.ExecutionContextExecutor
import akka.actor.Cancellable

/**
 * INTERNAL API
 */
private[typed] class ActorCell[T](override val system: ActorSystemImpl[Nothing],
                                  override val props: Props[T])
    extends ActorContext[T] with Runnable {

  /*
   * Implementation of the ActorContext trait.
   */

  private var childrenMap = Map.empty[String, ActorRef[Nothing]]
  override def children: Iterable[ActorRef[Nothing]] = childrenMap.values
  override def child(name: String): Option[ActorRef[Nothing]] = childrenMap.get(name)

  private var _self: ActorRef[T] = _
  private[typed] def setSelf(ref: ActorRef[T]): Unit = _self = ref
  override def self: ActorRef[T] = _self

  override def spawn[U](props: Props[U], name: String): ActorRef[U] = {
    if (childrenMap contains name) throw new InvalidActorNameException(s"actor name [$name] is not unique")
    val cell = new ActorCell[U](system, props)
    val ref = new LocalActorRef[U](self.path / name, cell)
    cell.setSelf(ref)
    ref.sendSystem(Create())
    ref
  }

  private var nextName = 0L
  override def spawnAnonymous[U](props: Props[U]): ActorRef[U] = {
    val name = Helpers.base64(nextName)
    nextName += 1
    spawn(props, name)
  }

  override def stop(child: ActorRef[Nothing]): Boolean =
    childrenMap get child.path.name match {
      case None                      => false
      case Some(ref) if ref != child => false
      case Some(ref) =>
        ref.toImplN.sendSystem(Terminate())
        true
    }

  override def watch[U](target: ActorRef[U]): ActorRef[U] = {
    target.toImpl.sendSystem(Watch(target, self))
    target
  }

  override def unwatch[U](target: ActorRef[U]): ActorRef[U] = {
    target.toImpl.sendSystem(Unwatch(target, self))
    target
  }

  private var receiveTimeout: Duration = Duration.Undefined
  override def setReceiveTimeout(d: Duration): Unit = receiveTimeout = d

  override def schedule[U](delay: FiniteDuration, target: ActorRef[U], msg: U): Cancellable =
    system.scheduler.scheduleOnce(delay)(target ! msg)(ExecutionContexts.sameThreadExecutionContext)

  override val executionContext: ExecutionContextExecutor = system.dispatchers.lookup(props.dispatcher)

  override def spawnAdapter[U](f: U ⇒ T): ActorRef[U] = ???

  /*
   * Implementation of the invocation mechanics.
   */

  /*
   * bit 0-20: activation count (number of (system)messages)
   * bit 21-30: suspend count
   * bit 31: isClosed
   */
  @volatile private var status = 0

  def send(msg: T): Unit = ???
  def sendSystem(signal: SystemMessage): Unit = ???

  def run(): Unit = {

  }
}
