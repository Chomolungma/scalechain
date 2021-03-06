package io.scalechain.blockchain.script

import io.kotlintest.KTestJUnitRunner
import io.scalechain.blockchain.ErrorCode
import io.scalechain.blockchain.script.ops.*
import io.scalechain.util.HexUtil
import org.junit.runner.RunWith

/** Test crypto operations in Crypto.scala
  *
  */
@RunWith(KTestJUnitRunner::class)
class CryptoSpec : OperationTestTrait() {

  val operations =
    table(
      // column names
      headers("inputValues", "operation", "expectedOutputValue"),
      // test cases with input value, script operation, output value
      // The input value is pushed on to the script execution stack from left to right.

      // OP_RIPEMD160(0xa6) : Return RIPEMD160 hash of top item
      // Before : in
      // After  : hash
      stackTest( stack("Hello World!"), OpRIPEMD160(), stack(HexUtil.bytes("8476ee4631b9b30ac2754b0ee0c47e161d3f724c"))),
      stackTest( stack(),               OpRIPEMD160(), ErrorCode.NotEnoughInput),

      // OP_SHA1(0xa7) : Return SHA1 hash of top item
      // Before : in
      // After  : hash
      stackTest( stack("Hello World!"), OpSHA1(), stack(HexUtil.bytes("2ef7bde608ce5404e97d5f042f95f89f1c232871"))),
      stackTest( stack(),               OpSHA1(), ErrorCode.NotEnoughInput),

      // OP_SHA256(0xa8) : Return SHA256 hash of top item
      // Before : in
      // After  : hash
      stackTest( stack("Hello World!"), OpSHA256(), stack(HexUtil.bytes("7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069"))),
      stackTest( stack(),               OpSHA256(), ErrorCode.NotEnoughInput),

      // OP_HASH160(0xa9) : Return RIPEMD160(SHA256(x)) hash of top item
      // Before : in
      // After  : hash
      // BUGBUG : The expected value was copied from the actual output. Make sure it is correct.
      stackTest( stack("Hello World!"), OpHash160(), stack(HexUtil.bytes("85b0c1edc20ec4437ab5f936a7e1691498dd0a73"))),
      stackTest( stack(),               OpHash160(), ErrorCode.NotEnoughInput),

      // OP_HASH256(0xaa) : Return SHA256(SHA256(x)) hash of top item
      // Before : in
      // After  : hash
      // BUGBUG : The expected value was copied from the actual output. Make sure it is correct.
      stackTest( stack("Hello World!"), OpHash256(), stack(HexUtil.bytes("61f417374f4400b47dcae1a8f402d4f4dacf455a0442a06aa455a447b0d4e170"))),
      stackTest( stack(),               OpHash256(), ErrorCode.NotEnoughInput)
    )

    init {
        "operations" should "run and push expected value on the stack." {
            forAll(operations) { inputValues : Array<ScriptValue>, operation : ScriptOp, expectation : Any ->
                verifyOperations(inputValues, listOf(operation), expectation);
            }
        }
    }
}