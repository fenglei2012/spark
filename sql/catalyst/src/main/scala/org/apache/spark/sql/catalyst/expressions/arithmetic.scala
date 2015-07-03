/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.catalyst.expressions

import org.apache.spark.sql.catalyst.analysis.TypeCheckResult
import org.apache.spark.sql.catalyst.expressions.codegen.{CodeGenContext, GeneratedExpressionCode}
import org.apache.spark.sql.catalyst.util.TypeUtils
import org.apache.spark.sql.types._

<<<<<<< HEAD
case class UnaryMinus(child: Expression) extends UnaryExpression {
  type EvaluatedType = Any
=======
abstract class UnaryArithmetic extends UnaryExpression {
  self: Product =>
>>>>>>> upstream/master

  override def dataType: DataType = child.dataType

  override def eval(input: InternalRow): Any = {
    val evalE = child.eval(input)
    if (evalE == null) {
      null
    } else {
      evalInternal(evalE)
    }
  }

  protected def evalInternal(evalE: Any): Any =
    sys.error(s"UnaryArithmetics must override either eval or evalInternal")
}

<<<<<<< HEAD
case class Sqrt(child: Expression) extends UnaryExpression {
  type EvaluatedType = Any
=======
case class UnaryMinus(child: Expression) extends UnaryArithmetic {
  override def toString: String = s"-$child"
>>>>>>> upstream/master

  override def checkInputDataTypes(): TypeCheckResult =
    TypeUtils.checkForNumericExpr(child.dataType, "operator -")

  private lazy val numeric = TypeUtils.getNumeric(dataType)

  override def genCode(ctx: CodeGenContext, ev: GeneratedExpressionCode): String = dataType match {
    case dt: DecimalType => defineCodeGen(ctx, ev, c => s"$c.unary_$$minus()")
    case dt: NumericType => defineCodeGen(ctx, ev, c => s"(${ctx.javaType(dt)})(-($c))")
  }

  protected override def evalInternal(evalE: Any) = numeric.negate(evalE)
}

case class UnaryPositive(child: Expression) extends UnaryArithmetic {
  override def prettyName: String = "positive"

  override def genCode(ctx: CodeGenContext, ev: GeneratedExpressionCode): String =
    defineCodeGen(ctx, ev, c => c)

  protected override def evalInternal(evalE: Any) = evalE
}

/**
 * A function that get the absolute value of the numeric value.
 */
case class Abs(child: Expression) extends UnaryArithmetic {
  override def checkInputDataTypes(): TypeCheckResult =
    TypeUtils.checkForNumericExpr(child.dataType, "function abs")

  private lazy val numeric = TypeUtils.getNumeric(dataType)

  protected override def evalInternal(evalE: Any) = numeric.abs(evalE)
}

abstract class BinaryArithmetic extends BinaryOperator {
  self: Product =>

<<<<<<< HEAD
  type EvaluatedType = Any

  override lazy val resolved =
    left.resolved && right.resolved &&
    left.dataType == right.dataType &&
    !DecimalType.isFixed(left.dataType)

  override def dataType: DataType = {
    if (!resolved) {
      throw new UnresolvedException(this,
        s"datatype. Can not resolve due to differing types ${left.dataType}, ${right.dataType}")
=======
  override def dataType: DataType = left.dataType

  override def checkInputDataTypes(): TypeCheckResult = {
    if (left.dataType != right.dataType) {
      TypeCheckResult.TypeCheckFailure(
        s"differing types in ${this.getClass.getSimpleName} " +
        s"(${left.dataType} and ${right.dataType}).")
    } else {
      checkTypesInternal(dataType)
>>>>>>> upstream/master
    }
  }

  protected def checkTypesInternal(t: DataType): TypeCheckResult

  override def eval(input: InternalRow): Any = {
    val evalE1 = left.eval(input)
    if(evalE1 == null) {
      null
    } else {
      val evalE2 = right.eval(input)
      if (evalE2 == null) {
        null
      } else {
        evalInternal(evalE1, evalE2)
      }
    }
  }

<<<<<<< HEAD
  def evalInternal(evalE1: EvaluatedType, evalE2: EvaluatedType): Any =
    sys.error(s"BinaryExpressions must either override eval or evalInternal")
=======
  /** Name of the function for this expression on a [[Decimal]] type. */
  def decimalMethod: String =
    sys.error("BinaryArithmetics must override either decimalMethod or genCode")

  override def genCode(ctx: CodeGenContext, ev: GeneratedExpressionCode): String = dataType match {
    case dt: DecimalType =>
      defineCodeGen(ctx, ev, (eval1, eval2) => s"$eval1.$decimalMethod($eval2)")
    // byte and short are casted into int when add, minus, times or divide
    case ByteType | ShortType =>
      defineCodeGen(ctx, ev,
        (eval1, eval2) => s"(${ctx.javaType(dataType)})($eval1 $symbol $eval2)")
    case _ =>
      defineCodeGen(ctx, ev, (eval1, eval2) => s"$eval1 $symbol $eval2")
  }

  protected def evalInternal(evalE1: Any, evalE2: Any): Any =
    sys.error(s"BinaryArithmetics must override either eval or evalInternal")
}

private[sql] object BinaryArithmetic {
  def unapply(e: BinaryArithmetic): Option[(Expression, Expression)] = Some((e.left, e.right))
>>>>>>> upstream/master
}

case class Add(left: Expression, right: Expression) extends BinaryArithmetic {
  override def symbol: String = "+"
  override def decimalMethod: String = "$plus"

  override lazy val resolved =
    childrenResolved && checkInputDataTypes().isSuccess && !DecimalType.isFixed(dataType)

  protected def checkTypesInternal(t: DataType) =
    TypeUtils.checkForNumericExpr(t, "operator " + symbol)

  private lazy val numeric = TypeUtils.getNumeric(dataType)

  protected override def evalInternal(evalE1: Any, evalE2: Any) = numeric.plus(evalE1, evalE2)
}

case class Subtract(left: Expression, right: Expression) extends BinaryArithmetic {
  override def symbol: String = "-"
  override def decimalMethod: String = "$minus"

  override lazy val resolved =
    childrenResolved && checkInputDataTypes().isSuccess && !DecimalType.isFixed(dataType)

  protected def checkTypesInternal(t: DataType) =
    TypeUtils.checkForNumericExpr(t, "operator " + symbol)

  private lazy val numeric = TypeUtils.getNumeric(dataType)

  protected override def evalInternal(evalE1: Any, evalE2: Any) = numeric.minus(evalE1, evalE2)
}

case class Multiply(left: Expression, right: Expression) extends BinaryArithmetic {
  override def symbol: String = "*"
  override def decimalMethod: String = "$times"

  override lazy val resolved =
    childrenResolved && checkInputDataTypes().isSuccess && !DecimalType.isFixed(dataType)

  protected def checkTypesInternal(t: DataType) =
    TypeUtils.checkForNumericExpr(t, "operator " + symbol)

  private lazy val numeric = TypeUtils.getNumeric(dataType)

  protected override def evalInternal(evalE1: Any, evalE2: Any) = numeric.times(evalE1, evalE2)
}

case class Divide(left: Expression, right: Expression) extends BinaryArithmetic {
  override def symbol: String = "/"
  override def decimalMethod: String = "$div"

  override def nullable: Boolean = true

  override lazy val resolved =
    childrenResolved && checkInputDataTypes().isSuccess && !DecimalType.isFixed(dataType)

  protected def checkTypesInternal(t: DataType) =
    TypeUtils.checkForNumericExpr(t, "operator " + symbol)

  private lazy val div: (Any, Any) => Any = dataType match {
    case ft: FractionalType => ft.fractional.asInstanceOf[Fractional[Any]].div
    case it: IntegralType => it.integral.asInstanceOf[Integral[Any]].quot
  }

  override def eval(input: InternalRow): Any = {
    val evalE2 = right.eval(input)
    if (evalE2 == null || evalE2 == 0) {
      null
    } else {
      val evalE1 = left.eval(input)
      if (evalE1 == null) {
        null
      } else {
        div(evalE1, evalE2)
      }
    }
  }

  /**
   * Special case handling due to division by 0 => null.
   */
  override def genCode(ctx: CodeGenContext, ev: GeneratedExpressionCode): String = {
    val eval1 = left.gen(ctx)
    val eval2 = right.gen(ctx)
    val isZero = if (dataType.isInstanceOf[DecimalType]) {
      s"${eval2.primitive}.isZero()"
    } else {
      s"${eval2.primitive} == 0"
    }
    val javaType = ctx.javaType(dataType)
    val divide = if (dataType.isInstanceOf[DecimalType]) {
      s"${eval1.primitive}.$decimalMethod(${eval2.primitive})"
    } else {
      s"($javaType)(${eval1.primitive} $symbol ${eval2.primitive})"
    }
    s"""
      ${eval2.code}
      boolean ${ev.isNull} = false;
      $javaType ${ev.primitive} = ${ctx.defaultValue(javaType)};
      if (${eval2.isNull} || $isZero) {
        ${ev.isNull} = true;
      } else {
        ${eval1.code}
        if (${eval1.isNull}) {
          ${ev.isNull} = true;
        } else {
          ${ev.primitive} = $divide;
        }
      }
    """
  }
}

case class Remainder(left: Expression, right: Expression) extends BinaryArithmetic {
  override def symbol: String = "%"
  override def decimalMethod: String = "remainder"

  override def nullable: Boolean = true

  override lazy val resolved =
    childrenResolved && checkInputDataTypes().isSuccess && !DecimalType.isFixed(dataType)

  protected def checkTypesInternal(t: DataType) =
    TypeUtils.checkForNumericExpr(t, "operator " + symbol)

  private lazy val integral = dataType match {
    case i: IntegralType => i.integral.asInstanceOf[Integral[Any]]
    case i: FractionalType => i.asIntegral.asInstanceOf[Integral[Any]]
  }

  override def eval(input: InternalRow): Any = {
    val evalE2 = right.eval(input)
    if (evalE2 == null || evalE2 == 0) {
      null
    } else {
      val evalE1 = left.eval(input)
      if (evalE1 == null) {
        null
      } else {
        integral.rem(evalE1, evalE2)
      }
    }
  }
<<<<<<< HEAD
}

/**
 * A function that calculates bitwise and(&) of two numbers.
 */
case class BitwiseAnd(left: Expression, right: Expression) extends BinaryArithmetic {
  override def symbol: String = "&"

  lazy val and: (Any, Any) => Any = dataType match {
    case ByteType =>
      ((evalE1: Byte, evalE2: Byte) => (evalE1 & evalE2).toByte).asInstanceOf[(Any, Any) => Any]
    case ShortType =>
      ((evalE1: Short, evalE2: Short) => (evalE1 & evalE2).toShort).asInstanceOf[(Any, Any) => Any]
    case IntegerType =>
      ((evalE1: Int, evalE2: Int) => evalE1 & evalE2).asInstanceOf[(Any, Any) => Any]
    case LongType =>
      ((evalE1: Long, evalE2: Long) => evalE1 & evalE2).asInstanceOf[(Any, Any) => Any]
    case other => sys.error(s"Unsupported bitwise & operation on $other")
  }

  override def evalInternal(evalE1: EvaluatedType, evalE2: EvaluatedType): Any = and(evalE1, evalE2)
}

/**
 * A function that calculates bitwise or(|) of two numbers.
 */
case class BitwiseOr(left: Expression, right: Expression) extends BinaryArithmetic {
  override def symbol: String = "|"

  lazy val or: (Any, Any) => Any = dataType match {
    case ByteType =>
      ((evalE1: Byte, evalE2: Byte) => (evalE1 | evalE2).toByte).asInstanceOf[(Any, Any) => Any]
    case ShortType =>
      ((evalE1: Short, evalE2: Short) => (evalE1 | evalE2).toShort).asInstanceOf[(Any, Any) => Any]
    case IntegerType =>
      ((evalE1: Int, evalE2: Int) => evalE1 | evalE2).asInstanceOf[(Any, Any) => Any]
    case LongType =>
      ((evalE1: Long, evalE2: Long) => evalE1 | evalE2).asInstanceOf[(Any, Any) => Any]
    case other => sys.error(s"Unsupported bitwise | operation on $other")
  }

  override def evalInternal(evalE1: EvaluatedType, evalE2: EvaluatedType): Any = or(evalE1, evalE2)
}

/**
 * A function that calculates bitwise xor(^) of two numbers.
 */
case class BitwiseXor(left: Expression, right: Expression) extends BinaryArithmetic {
  override def symbol: String = "^"

  lazy val xor: (Any, Any) => Any = dataType match {
    case ByteType =>
      ((evalE1: Byte, evalE2: Byte) => (evalE1 ^ evalE2).toByte).asInstanceOf[(Any, Any) => Any]
    case ShortType =>
      ((evalE1: Short, evalE2: Short) => (evalE1 ^ evalE2).toShort).asInstanceOf[(Any, Any) => Any]
    case IntegerType =>
      ((evalE1: Int, evalE2: Int) => evalE1 ^ evalE2).asInstanceOf[(Any, Any) => Any]
    case LongType =>
      ((evalE1: Long, evalE2: Long) => evalE1 ^ evalE2).asInstanceOf[(Any, Any) => Any]
    case other => sys.error(s"Unsupported bitwise ^ operation on $other")
  }

  override def evalInternal(evalE1: EvaluatedType, evalE2: EvaluatedType): Any = xor(evalE1, evalE2)
}

/**
 * A function that calculates bitwise not(~) of a number.
 */
case class BitwiseNot(child: Expression) extends UnaryExpression {
  type EvaluatedType = Any

  override def dataType: DataType = child.dataType
  override def foldable: Boolean = child.foldable
  override def nullable: Boolean = child.nullable
  override def toString: String = s"~$child"

  lazy val not: (Any) => Any = dataType match {
    case ByteType =>
      ((evalE: Byte) => (~evalE).toByte).asInstanceOf[(Any) => Any]
    case ShortType =>
      ((evalE: Short) => (~evalE).toShort).asInstanceOf[(Any) => Any]
    case IntegerType =>
      ((evalE: Int) => ~evalE).asInstanceOf[(Any) => Any]
    case LongType =>
      ((evalE: Long) => ~evalE).asInstanceOf[(Any) => Any]
    case other => sys.error(s"Unsupported bitwise ~ operation on $other")
  }
=======
>>>>>>> upstream/master

  /**
   * Special case handling for x % 0 ==> null.
   */
  override def genCode(ctx: CodeGenContext, ev: GeneratedExpressionCode): String = {
    val eval1 = left.gen(ctx)
    val eval2 = right.gen(ctx)
    val isZero = if (dataType.isInstanceOf[DecimalType]) {
      s"${eval2.primitive}.isZero()"
    } else {
      s"${eval2.primitive} == 0"
    }
    val javaType = ctx.javaType(dataType)
    val remainder = if (dataType.isInstanceOf[DecimalType]) {
      s"${eval1.primitive}.$decimalMethod(${eval2.primitive})"
    } else {
      s"($javaType)(${eval1.primitive} $symbol ${eval2.primitive})"
    }
    s"""
      ${eval2.code}
      boolean ${ev.isNull} = false;
      $javaType ${ev.primitive} = ${ctx.defaultValue(javaType)};
      if (${eval2.isNull} || $isZero) {
        ${ev.isNull} = true;
      } else {
        ${eval1.code}
        if (${eval1.isNull}) {
          ${ev.isNull} = true;
        } else {
          ${ev.primitive} = $remainder;
        }
      }
    """
  }
}

<<<<<<< HEAD
case class MaxOf(left: Expression, right: Expression) extends Expression {
  type EvaluatedType = Any

  override def foldable: Boolean = left.foldable && right.foldable

=======
case class MaxOf(left: Expression, right: Expression) extends BinaryArithmetic {
>>>>>>> upstream/master
  override def nullable: Boolean = left.nullable && right.nullable

  protected def checkTypesInternal(t: DataType) =
    TypeUtils.checkForOrderingExpr(t, "function maxOf")

  private lazy val ordering = TypeUtils.getOrdering(dataType)

  override def eval(input: InternalRow): Any = {
    val evalE1 = left.eval(input)
    val evalE2 = right.eval(input)
    if (evalE1 == null) {
      evalE2
    } else if (evalE2 == null) {
      evalE1
    } else {
      if (ordering.compare(evalE1, evalE2) < 0) {
        evalE2
      } else {
        evalE1
      }
    }
  }

<<<<<<< HEAD
  override def toString: String = s"MaxOf($left, $right)"
}

case class MinOf(left: Expression, right: Expression) extends Expression {
  type EvaluatedType = Any
=======
  override def genCode(ctx: CodeGenContext, ev: GeneratedExpressionCode): String = {
    val eval1 = left.gen(ctx)
    val eval2 = right.gen(ctx)
    val compCode = ctx.genComp(dataType, eval1.primitive, eval2.primitive)

    eval1.code + eval2.code + s"""
      boolean ${ev.isNull} = false;
      ${ctx.javaType(left.dataType)} ${ev.primitive} =
        ${ctx.defaultValue(left.dataType)};

      if (${eval1.isNull}) {
        ${ev.isNull} = ${eval2.isNull};
        ${ev.primitive} = ${eval2.primitive};
      } else if (${eval2.isNull}) {
        ${ev.isNull} = ${eval1.isNull};
        ${ev.primitive} = ${eval1.primitive};
      } else {
        if ($compCode > 0) {
          ${ev.primitive} = ${eval1.primitive};
        } else {
          ${ev.primitive} = ${eval2.primitive};
        }
      }
    """
  }
>>>>>>> upstream/master

  override def symbol: String = "max"
  override def prettyName: String = symbol
}

case class MinOf(left: Expression, right: Expression) extends BinaryArithmetic {
  override def nullable: Boolean = left.nullable && right.nullable

  protected def checkTypesInternal(t: DataType) =
    TypeUtils.checkForOrderingExpr(t, "function minOf")

  private lazy val ordering = TypeUtils.getOrdering(dataType)

  override def eval(input: InternalRow): Any = {
    val evalE1 = left.eval(input)
    val evalE2 = right.eval(input)
    if (evalE1 == null) {
      evalE2
    } else if (evalE2 == null) {
      evalE1
    } else {
      if (ordering.compare(evalE1, evalE2) < 0) {
        evalE1
      } else {
        evalE2
      }
    }
  }

<<<<<<< HEAD
  override def toString: String = s"MinOf($left, $right)"
}

/**
 * A function that get the absolute value of the numeric value.
 */
case class Abs(child: Expression) extends UnaryExpression  {
  type EvaluatedType = Any

  override def dataType: DataType = child.dataType
  override def foldable: Boolean = child.foldable
  override def nullable: Boolean = child.nullable
  override def toString: String = s"Abs($child)"

  lazy val numeric = dataType match {
    case n: NumericType => n.numeric.asInstanceOf[Numeric[Any]]
    case other => sys.error(s"Type $other does not support numeric operations")
=======
  override def genCode(ctx: CodeGenContext, ev: GeneratedExpressionCode): String = {
    val eval1 = left.gen(ctx)
    val eval2 = right.gen(ctx)
    val compCode = ctx.genComp(dataType, eval1.primitive, eval2.primitive)

    eval1.code + eval2.code + s"""
      boolean ${ev.isNull} = false;
      ${ctx.javaType(left.dataType)} ${ev.primitive} =
        ${ctx.defaultValue(left.dataType)};

      if (${eval1.isNull}) {
        ${ev.isNull} = ${eval2.isNull};
        ${ev.primitive} = ${eval2.primitive};
      } else if (${eval2.isNull}) {
        ${ev.isNull} = ${eval1.isNull};
        ${ev.primitive} = ${eval1.primitive};
      } else {
        if ($compCode < 0) {
          ${ev.primitive} = ${eval1.primitive};
        } else {
          ${ev.primitive} = ${eval2.primitive};
        }
      }
    """
>>>>>>> upstream/master
  }

  override def symbol: String = "min"
  override def prettyName: String = symbol
}
