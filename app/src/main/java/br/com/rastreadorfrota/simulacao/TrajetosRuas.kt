package br.com.rastreadorfrota.simulacao

/*
 * Trajetos POR RUAS das viagens simuladas, gerados UMA vez no OSRM e guardados aqui
 * como polyline codificada (precisão 5). A simulação não acessa a rede: os dados são fixos.
 *
 * Gerados com (CD = -48.204879,-21.815086; destino = lon,lat da entrega):
 *   curl "https://router.project-osrm.org/route/v1/driving/{CD};{destino}?overview=full&geometries=polyline"
 * e o inverso ({destino};{CD}) para a volta. Campo usado: routes[0].geometry.
 */

internal const val TRAJETO_IDA_1 = """hwcdCn_feH??PJRF~BpDhAfBVVDDFDF@F@t@d@NLRNVHb@Dn@BNBNDJFLFHHbD~FBH?F?H?H?FEFCFGDEDGBI@G?IAGAGCGEEEiA_CUq@Yw@Qo@U{@Qu@e@gCU{AQwAIkAEa@EuAG_B?}@@yB@oBH}F@aCBiAB{B@eAT}RBkAR}N@m@F{FP{N@y@?KDuBFeEB}BBgABcAHaBLaBVeCXkB\}Bf@{Cx@iF`@_CRgARo@\q@NQHYd@y@Pi@Dk@CWAYIa@KSS_@USg@WWEq@EYAYF{@Gi@O_@Qc@_@e@q@{@{AUq@Wo@e@iBaAwGaB{L[aD[wCg@kDyBiPuEe_@MqA_BoNMgAKgBBa@DUHOPQPIZCV@TFVLNLJRDT?RAXEPIPKLMJc@Tq@LsB^}@NaFz@i@Na@N[N[P_@TWVgAdAaA|@[XyAvAcF~EKJyAmB}AqBdAaA"""

internal const val TRAJETO_VOLTA_1 = """pebdC|h}dH|GoGzAfB|AjBJL`A}@hAeAXUVQRMl@YTGBAbAN\Jz@h@|A~@z@l@`@^`@h@Zn@L`@H`@N~@h@|CpDtYVxBtBbOh@~DZlC\vChB`Oz@|Ed@fB|@tB~@zAL\FTDV?^CPCNIZCVKROXEPITGTCTCZ@rABL?v@G\Ih@k@lAwD`H}@bBy@zAyAhC_CfEuHfLcDbFyGfKyD|FcCxDqChEcAvAi@f@sAdA]XYXaEfGSZSZ[b@cCvD_A`B]NIBI?CACACAG?G@E@EDEDCDAJ@JDHDFHBH@@?HLHFJFBBt@f@DDRd@DP@HDHDFBBFBH@H?JEHIFK@M@MFc@XaAf@_BP[d@{@rCiERSVMVIVEZCn@At@@p@Fp@J~@V|@^z@j@hAhAtUf_@rI|MLb@Lb@HVLRNPNL"""

internal const val TRAJETO_IDA_2 = """hwcdCn_feH??PJRF~BpDhAfBVVDDFDF@F@t@d@NLRNVHb@Dn@BNBNDJFLFHHbD~FBH?F?H?H?FEFCFGDEDGBI@G?IAGAGCGEEEiA_CUq@Yw@Qo@U{@Qu@e@gCU{AQwAIkAEa@EuAG_B?}@@yB@oBH}F@aCBiAB{B@eAT}RBkAR}N@m@F{FP{N@y@?KDuBFeEB}BBgABcAHaBLaBVeCXkB\}Bf@{Cx@iF`@_CRgARo@\q@NQHYd@y@Pi@Dk@CWAYIa@KSS_@USg@WWEq@EYAYFmBn@u@Zs@`@KRi@LUBuA@qBEgCMiCSa@EoFg@u@KUGOEc@SMGKAMAS@UBSBY?C?UCmBYSCMCyBUiAIC?gEc@yC]i@Eu@OWEUCqAUmCa@SEwDe@m@?a@?o@@g@BM?N|@qAX[FkAVkDv@w@mDEMu@uDCGu@}Dk@iCMa@?I?K@IRADG@IAIEIO?O@MD]FSBI?qBAoFQwBKcBKg@C}@E_@CYESIECWKICMAS?Q@a@D[?c@Cs@CeBOaBOK?GAo@Cu@@{@HG?IAIEQGMCs@?WB]B]E[Ee@OYGYA[Ae@?}AAcDI}FKiC@iBAmEBiA?oA@k@Ca@?QAS@aABW@eABC?aAAc@?c@EsA[}@SaBe@aB_@q@Qm@QiAi@QKYMUI{A]k@SsDsBiE_Ci@YKGGEm@YQMQFGHEJ?NINGPcAtAsFlGKLuCjDMNqCbDSXeBrBMNYNKAM@IDIHEJAJ?J@HELOPgBzBKJgBrBQPKLeBpBIJyAhBq@v@aAfAy@?{BkFw@gB"""

internal const val TRAJETO_VOLTA_2 = """~|zcChk`eH_AwBtDoBhBjEJXtBtEFJ|AiBFKhBoBJMvBgCHMfBqBXURCL?JCHGFKBM?KCML[RUbAkAZ]dD_ELOvCiDJOrFoG|@yALKPO^NZFj@Hd@P`DfBTLjCrAt@d@JHn@f@d@^JHRHf@NbBD|@Rz@Rv@TfBj@fATpAThAH~AAf@?^BB?j@BLBL@L?LAz@EnAGtAEvD?bFCjA?~EJjADlBDhB@H?`@@PBTJ^ZVNNBRCVKLGTGd@APBPHHDH?NA^E`@En@A\@ZBJ?dBNf@Dv@FbAHZDj@TLFPFJLDFHJLp@x@fEl@zCVjA^rBRbA^xBRdA\lBDRLt@d@`CXxAr@xDr@tDh@vCN~@r@dEZ`BDVPlAb@|B^vBbAnFv@pE?PALA@GJCL@LBLFFHFNPHRHVJb@f@jCjAvG@VATCR?RBPFRJLVh@HXH\@Hj@`DHb@v@nEp@lDBH~@hFp@vD@PBTAJ@JDHDFHBH@@?HLHFJFBBt@f@DDRd@DP@HDHDFBBFBH@H?JEHIFK@M@MFc@XaAf@_BP[d@{@rCiERSVMVIVEZCn@At@@p@Fp@J~@V|@^z@j@hAhAtUf_@rI|MLb@Lb@HVLRNPNL"""

internal const val TRAJETO_IDA_3 = """hwcdCn_feH??PJRF~BpDhAfBVVDDFDF@F@HAFCDCDGBG@I?GAICGEEGEGCG?_@GUIQMMQOOyQsYkL_R}GuKq@y@y@o@_Ai@u@Uy@Sg@Gg@Ei@Ai@@u@DYBWFWJSLy@dAcCzDYf@Sh@u@zBEHMHKFC?I?K@IAe@KEEu@g@CCGMAK?ODI@KAICKGGCACACAQQMWo@cDo@iDIg@y@oEk@{CMs@y@oEG_@I_@?W@SFUAIAOCMGMGKKMKMGOiAsGIa@O_ASeAAU?_@?K?]@E?ICKEGEGIECAI?I?GBGDA@wB\}@NeFfAsEdA_B^oBd@wAZ_@FyAZsBb@eB\SDYFmFfAcCh@WFcCh@eCh@YHUD[H]FQD]HMy@Kk@OcAKm@s@aFAM_@mCCOa@kCCQi@mDCMk@sDCMm@oDAKo@uDCIk@sDEMa@kCKk@e@aDCOYaBUsACMk@qDAGqA_LhDk@v@nF"""

internal const val TRAJETO_VOLTA_3 = """xo~cC~v`eHBTLx@RnAZ|BLj@@J@HXfB@JFZDXBJ@JVxBJp@^dCLz@j@`EXnBHd@Hh@j@|D`@jCLv@j@zDd@~CXnBHj@t@nFHb@Hj@NbAP`ADXjGuAzCm@lFgAVGxBe@tBc@dASPExBc@nBa@`B]pEiAdFiAh@ITG^INCL?L?JBNFFFHFNPHRHVJb@f@jCjAvG@VATCR?RBPFRJLVh@HXH\@Hj@`DHb@v@nEp@lDBH~@hFp@vD@PBTAJ@JDHDFHBH@@?HLHFJFBBt@f@DDRd@DP@HDHDFBBFBH@H?JEHIFK@M@MFc@XaAf@_BP[d@{@rCiERSVMVIVEZCn@At@@p@Fp@J~@V|@^z@j@hAhAtUf_@rI|MLb@Lb@HVLRNPNL"""

internal const val TRAJETO_IDA_4 = """hwcdCn_feH??PJRF~BpDhAfBVVDDFDF@F@t@d@NLRNVHb@Dn@BNBNDJFLFHHbD~FBH?F?H?H?FEFCFGDEDGBI@G?IAGAGCGEEEiA_CUq@Yw@Qo@U{@Qu@e@gCU{AQwAIkAEa@EuAG_B?}@@yB@oBH}F@aCBiAB{B@eAT}RBkAR}N@m@F{FP{N@y@?KDuBFeEB}BBgABcAHaBLaBVeCXkB\}Bf@{Cx@iF`@_CRgARo@\q@NQHYd@y@Pi@Dk@CWAYIa@KSS_@USg@WWEq@EYAYF{@Gi@O_@Qc@_@e@q@{@{AUq@Wo@e@iBaAwGaB{L[aD[wCg@kDyBiPuEe_@MqA_BoNMgAc@_DGg@U}@_@eAc@w@u@qA]_@a@c@{@s@WMuBeBeCuBaCsBoAeAiJeIs@m@cFmEmHiHyEkFqEiGoDuF{DiGyEiJaOoZYi@gCkFuEuJ{AgDgA_CwBeEyAuCeAgB_@oACM?MDOFIDCNCL@NHr@p@t@|@xEjIJZDTBV?VAHQTIJqBnAQDKBODGCI@GBGFCH?FGRSd@W\qAv@cA|@]`@o@r@u@`Ak@x@Q\_@r@a@|@]bAYbAWnAa@dBUdBCZEVEj@AZEhA@t@@x@BbB?ZDlB^vM@\?d@?b@Gb@Ib@Gb@CFCH?HBHDHFNL\Lb@DNBRBn@@RBv@XxLPtFD~B"""

internal const val TRAJETO_VOLTA_4 = """f__dCdh{dHd@xRAX?H@JB\?~@BnAB`AFlB@v@GxACNCJGXUp@y@tAEREFKHs@b@EFAHTlDLzBVvENdD?`DNz@zAjKFf@fAlHL~@l@|Dd@tDZdCJt@|A~KrACPFR^Lp@PtABRBN~BQNKpCShBK~Hc@J~BFnA?\I`@KZAL?J@L@J@FBHJLDFHJLp@x@fEl@zCVjA^rBRbA^xBRdA\lBDRLt@d@`CXxAr@xDr@tDh@vCN~@r@dEZ`BDVPlAb@|B^vBbAnFv@pE?PALA@GJCL@LBLFFHFNPHRHVJb@f@jCjAvG@VATCR?RBPFRJLVh@HXH\@Hj@`DHb@v@nEp@lDBH~@hFp@vD@PBTAJ@JDHDFHBH@@?HLHFJFBBt@f@DDRd@DP@HDHDFBBFBH@H?JEHIFK@M@MFc@XaAf@_BP[d@{@rCiERSVMVIVEZCn@At@@p@Fp@J~@V|@^z@j@hAhAtUf_@rI|MLb@Lb@HVLRNPNL"""
