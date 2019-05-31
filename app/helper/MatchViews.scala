package helper

object MatchViews {
  object StringList {
    private def isString(x:Any):Boolean = x match {
      case _:String => true
      case _ => false
    }

    def unapply(x:Any):Option[List[String]] = x match {
      case Nil => Some(List[String]())
      case lst:List[_] if lst.forall(isString _) => Some(lst.asInstanceOf[List[String]])
      case _ => None
    }
  }
}

