/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.lang.java.types.internal.infer

import net.sourceforge.pmd.lang.test.ast.shouldBe
import net.sourceforge.pmd.lang.java.ast.*
import net.sourceforge.pmd.lang.java.ast.JavaVersion.*
import net.sourceforge.pmd.lang.java.symbols.JConstructorSymbol
import net.sourceforge.pmd.lang.java.types.*

/**
 * @author Clément Fournier
 */
class Java7InferenceTest : ProcessorTestSpec({

    parserTest("Same test in java 8", javaVersion = J1_8) {
        val (acu, spy) = parser.parseWithTypeInferenceSpy(
            """
            class Gen<T> {
                Gen(T t) {}
                static {
                    // inferred to Gen<Class<?>>
                    Gen<Class<?>> g = new Gen<>(String.class);
                }
            }
            """
        )

        val (t_Gen) = acu.descendants(ASTTypeDeclaration::class.java).toList { it.typeMirror }

        val (genCall) = acu.descendants(ASTConstructorCall::class.java).toList()

        spy.shouldBeOk {
            ctorInfersTo(genCall, inferredType = t_Gen[Class::class[`?`]])
        }
    }

    parserTest("Java 7 uses return constraints if needed", javaVersion = J1_8) {
        val (acu, spy) = parser.parseWithTypeInferenceSpy(
            """
            class Gen<T> {
                static {
                    // inferred to Gen<Class<?>>
                    Gen<Class<?>> g = new Gen<>();
                }
            }
            """
        )

        val (t_Gen) = acu.descendants(ASTTypeDeclaration::class.java).toList { it.typeMirror }

        val (genCall) = acu.descendants(ASTConstructorCall::class.java).toList()

        spy.shouldBeOk {
            ctorInfersTo(genCall, inferredType = t_Gen[Class::class[`?`]])
        }
    }

})

private fun ctorInfersTo(
    call: ASTConstructorCall,
    inferredType: JClassType
) {
    call.methodType.shouldMatchMethod(
        named = JConstructorSymbol.CTOR_NAME,
        declaredIn = inferredType,
        returning = inferredType
    )
}

private fun methodInfersTo(call: ASTMethodCall, returnType: JClassType) {
    call.methodType.shouldMatchMethod(
        named = call.methodName,
        declaredIn = null, // not asserted
        returning = returnType
    )
}
