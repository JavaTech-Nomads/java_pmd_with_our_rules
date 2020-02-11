/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */


package net.sourceforge.pmd.lang.java.types

import io.kotlintest.shouldBe
import net.sourceforge.pmd.lang.ast.test.shouldBe
import net.sourceforge.pmd.lang.ast.test.shouldMatchN
import net.sourceforge.pmd.lang.java.ast.*

class InnerTypesTest : ProcessorTestSpec({


    parserTest("Test erased types ") {


        val acu = parser.parse("""
            class Scratch<T> {

                class Inner { }

                // this is implicitly Scratch<T>.Inner
                // in the raw type Scratch, it will be Scratch.Inner
                void doStuff(Inner t) { }
                
                void doStuffR(Scratch.Inner t) { }
                void doStuffP(Scratch<String>.Inner t) { }
                void doStuffD(Scratch<T>.Inner t) { } // this will be erased too 


                /*
                val doStuff = Scratch::class.raw.getMethodsByName("doStuff")[0]
                doStuff.formals[0].shouldBe Scratch::class.raw.selectInner(Inner::class) // Scratch.Inner, not Scratch<Object>.Inner

                Scratch::class.decl.getMethodsByName("doStuffR")[0].formals[0].shouldBe Scratch::class.raw.selectInner(Inner::class) // Scratch.Inner, not Scratch<T>.Inner
                Scratch::class[any()].getMethodsByName("doStuffP")[0].formals[0].shouldBe Scratch::class.raw.selectInner(Inner::class) // Scratch.Inner, not Scratch<T>.Inner
            */

        }

        """.trimIndent())

        val (scratch, inner) =
                acu.descendants(ASTAnyTypeDeclaration::class.java).toList()

        val (implicitlyDecl, explicitlyRaw, explicitlyParam, explicitlyDecl) =
                acu.descendants(ASTMethodDeclaration::class.java).map { it.formalParameters.toStream()[0]!! }.toList { it.typeMirror }


        val t_Scratch = scratch.typeMirror
        val t_Inner = inner.typeMirror

        t_Scratch::isGenericTypeDeclaration shouldBe true
        t_Inner::getEnclosingType shouldBe t_Scratch

        with(scratch.typeDsl) {

            doTest("Test erasure erases enclosing types") {
                t_Inner.erasure shouldBe t_Scratch.erasure.selectInner(inner.symbol, emptyList())
                t_Inner.erasure.toString() shouldBe "Scratch#Inner"
                t_Inner.erasure::isRaw shouldBe true
            }

            doTest("Test implicit type decl resolution in AST") {
                implicitlyDecl shouldBe t_Inner
                t_Scratch.getMethodsByName("doStuff")[0].formalParameters[0] shouldBe implicitlyDecl
                explicitlyRaw shouldBe t_Inner.erasure
                t_Scratch.getMethodsByName("doStuffR")[0].formalParameters[0] shouldBe explicitlyRaw
                explicitlyParam shouldBe t_Scratch[ts.STRING].selectInner(inner.symbol, emptyList())
                t_Scratch.getMethodsByName("doStuffP")[0].formalParameters[0] shouldBe explicitlyParam
                explicitlyDecl shouldBe t_Scratch.selectInner(inner.symbol, emptyList())
                explicitlyDecl shouldBe t_Inner
                t_Scratch.getMethodsByName("doStuffD")[0].formalParameters[0] shouldBe explicitlyDecl
            }

            doTest("Test erasure erases formal params ") {
                // todo fields
                val raw = t_Scratch.erasure

                raw.getMethodsByName("doStuff")[0].formalParameters[0] shouldBe t_Inner.erasure
                raw.getMethodsByName("doStuffR")[0].formalParameters[0] shouldBe t_Inner.erasure
                raw.getMethodsByName("doStuffP")[0].formalParameters[0] shouldBe explicitlyParam
                raw.getMethodsByName("doStuffD")[0].formalParameters[0] shouldBe t_Inner.erasure

            }
        }
    }

    parserTest("Test erased types overload resolution") {


        val acu = parser.parse("""
            class Scratch<T> {

                class Inner { }

                void doStuff(Inner t) { }
                void doStuff(Object t) { }

                {
                  Scratch rawScratch = new Scratch();
                  Scratch.Inner raw = rawScratch.new Inner();
                  Scratch<String>.Inner notRaw = new Scratch<String>().new Inner();
                  doStuff(raw);
                  rawScratch.doStuff(notRaw);
                }
        }

        """.trimIndent())

        val (scratch, inner) =
                acu.descendants(ASTAnyTypeDeclaration::class.java).toList()

        val (call, rawCall) = acu.descendants(ASTMethodCall::class.java).toList()


        val t_Scratch = scratch.typeMirror
        val t_Inner = inner.typeMirror



        call.shouldMatchN {
            methodCall("doStuff") {
                it.methodType.formalParameters[0].shouldBe(t_Inner)
                argList {
                    variableAccess("raw") {
                        it.typeMirror.shouldBe(t_Inner.erasure)
                    }
                }
            }
        }

        rawCall.shouldMatchN {
            methodCall("doStuff") {
                it.methodType.formalParameters[0].shouldBe(t_Inner.erasure)

                variableAccess("rawScratch") {
                    it.typeMirror.shouldBe(t_Scratch.erasure)
                }
                argList {
                    variableAccess("notRaw") {
                        it.typeMirror shouldBe with(it.typeDsl) {
                            t_Scratch[ts.STRING].selectInner(inner.symbol, emptyList())
                        }
                    }
                }
            }
        }
    }


})
