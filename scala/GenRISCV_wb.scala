package vexriscv.demo

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi.{Axi4ReadOnly, Axi4SpecRenamer}
import spinal.lib.cpu.riscv.debug.DebugTransportModuleParameter
import spinal.lib.eda.altera.{InterruptReceiverTag, ResetEmitterTag}
import spinal.lib.bus.amba4.axilite.AxiLite4SpecRenamer
import spinal.lib.misc.WishboneClint
import spinal.lib.misc.plic.WishbonePlic
import vexriscv.ip.{DataCacheConfig, InstructionCacheConfig}
import vexriscv.plugin._
import vexriscv.{Riscv, VexRiscv, VexRiscvConfig, plugin}
import vexriscv.ip.fpu.FpuParameter

object GenRISCV_wb {
  def main(args: Array[String]) {
    val report = SpinalVerilog{
      val cpuConfig = VexRiscvConfig(
        plugins = List(
          new IBusCachedPlugin(
            resetVector = 0xA0000000l,
            prediction = DYNAMIC,
            config = InstructionCacheConfig(
              cacheSize = 4096,
              bytePerLine =32,
              wayCount = 1,
              addressWidth = 32,
              cpuDataWidth = 32,
              memDataWidth = 32,
              catchIllegalAccess = true,
              catchAccessFault = true,
              asyncTagMemory = false,
              twoCycleRam = true,
              twoCycleCache = true
            ),
            memoryTranslatorPortConfig = MmuPortConfig(
              portTlbSize = 4
            )
          ),
          new DBusCachedPlugin(
            config = new DataCacheConfig(
              cacheSize         = 4096,
              bytePerLine       = 32,
              wayCount          = 1,
              addressWidth      = 32,
              cpuDataWidth      = 32,
              memDataWidth      = 32,
              catchAccessError  = true,
              catchIllegal      = true,
              catchUnaligned    = true
            ),
            memoryTranslatorPortConfig = MmuPortConfig(
              portTlbSize = 6
            )
          ),
          new StaticMemoryTranslatorPlugin(
            ioRange      = _(31 downto 29) === 0x7
          ),
          new DecoderSimplePlugin(
            catchIllegalInstruction = true
          ),
          new RegFilePlugin(
            regFileReadyKind = plugin.SYNC,
            zeroBoot = false
          ),
          new IntAluPlugin,
          new SrcPlugin(
            separatedAddSub = false,
            executeInsertion = true
          ),
          new FullBarrelShifterPlugin,
          new HazardSimplePlugin(
            bypassExecute           = true,
            bypassMemory            = true,
            bypassWriteBack         = true,
            bypassWriteBackBuffer   = true,
            pessimisticUseSrc       = false,
            pessimisticWriteRegFile = false,
            pessimisticAddressMatch = false
          ),
          new MulPlugin,
          new DivPlugin,
          new CsrPlugin(CsrPluginConfig.openSbi(mhartid = 0, misa = Riscv.misaToInt(s"imf")).copy(utimeAccess = CsrAccess.READ_ONLY, withPrivilegedDebug = true)),
          new EmbeddedRiscvJtag(
            p = DebugTransportModuleParameter(
              addressWidth = 7,
              version = 1,
              idle = 7
            ),
            debugCd = ClockDomain.current.copy(reset = Bool().setName("debugReset")),
            withTunneling = false,
            withTap = true
          ),
          new BranchPlugin(
            earlyBranch = false,
            catchAddressMisaligned = true
          ),
          new FpuPlugin(
              externalFpu = false,
              simHalt = false,
              p = FpuParameter(withDouble = false)
          ),
          new YamlPlugin("cpu0.yaml")
        )
      )
      val cpu = new VexRiscv(cpuConfig) {
        val clintCtrl = new WishboneClint(1) 
        val plicCtrl = new WishbonePlic(
          sourceCount = 31,
          targetCount = 2
        )

        val clint = clintCtrl.io.bus.toIo()
        val plic = plicCtrl.io.bus.toIo()
        val plicInterrupts = in Bits(32 bits)
        plicCtrl.io.sources := plicInterrupts >> 1


      }
      cpu.setDefinitionName("VexRiscv")
      cpu.rework {
        for (plugin <- cpuConfig.plugins) plugin match {
          case plugin: IBusSimplePlugin => {
            plugin.iBus.setAsDirectionLess() //Unset IO properties of iBus
            master(plugin.iBus.toWishbone()).setName("iBusWishbone")
          }
          case plugin: IBusCachedPlugin => {
            plugin.iBus.setAsDirectionLess()
            master(plugin.iBus.toWishbone()).setName("iBusWishbone")
          }
          case plugin: DBusSimplePlugin => {
            plugin.dBus.setAsDirectionLess()
            master(plugin.dBus.toWishbone()).setName("dBusWishbone")
          }
          case plugin: DBusCachedPlugin => {
            plugin.dBus.setAsDirectionLess()
            master(plugin.dBus.toWishbone()).setName("dBusWishbone")
          }
          case _ =>
        }
        }
      cpu
    }
  }
}
