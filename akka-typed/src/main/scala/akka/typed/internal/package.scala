package akka.typed

package object internal {
  implicit class ToImpl[U](val ref: ActorRef[U]) extends AnyVal {
    def toImpl: ActorRefImpl[U] = ref.asInstanceOf[ActorRefImpl[U]]
  }
  implicit class ToImplN(val ref: ActorRef[Nothing]) extends AnyVal {
    def toImplN: ActorRefImpl[Nothing] = ref.asInstanceOf[ActorRefImpl[Nothing]]
  }
}
