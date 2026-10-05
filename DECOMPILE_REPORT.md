# Decompile report

Sources were recovered from `original-jars/` with Vineflower and remapped to Mojang official names.

## viltrumiteflight

- Java files: 41
- Unmapped SRG names: 0
- Methods the decompiler couldn't fully recover: 0

## viltrumitecore

- Java files: 189
- Unmapped SRG names: 0
- Methods the decompiler couldn't fully recover: 0

## Compile check

❌ `./gradlew build` failed

```
/home/runner/work/viltrumite-mods/viltrumite-mods/viltrumiteflight/src/main/java/dev/baranhan/viltrumiteflight/client/mixin/PlayerRendererMixin.java:20: error: incompatible types: PlayerRendererMixin cannot be converted to PlayerRenderer
      PlayerRenderer renderer = (PlayerRenderer)this;
                                                ^
/home/runner/work/viltrumite-mods/viltrumite-mods/viltrumiteflight/src/main/java/dev/baranhan/viltrumiteflight/client/mixin/PlayerRendererMixin.java:35: error: incompatible types: PlayerRendererMixin cannot be converted to PlayerRenderer
      PlayerRenderer renderer = (PlayerRenderer)this;
                                                ^
/home/runner/work/viltrumite-mods/viltrumite-mods/viltrumiteflight/src/main/java/dev/baranhan/viltrumiteflight/client/mixin/SlowFlyingModelMixin.java:34: error: incompatible types: SlowFlyingModelMixin<T> cannot be converted to PlayerModel<?>
         PlayerModel<?> playerModel = (PlayerModel<?>)this;
                                                      ^
  where T is a type-variable:
--
/home/runner/work/viltrumite-mods/viltrumite-mods/viltrumiteflight/src/main/java/dev/baranhan/viltrumiteflight/client/mixin/PlayerFeatureRendererMixin.java:18: error: incompatible types: PlayerFeatureRendererMixin cannot be converted to PlayerRenderer
      PlayerRenderer renderer = (PlayerRenderer)this;
                                                ^
/home/runner/work/viltrumite-mods/viltrumite-mods/viltrumiteflight/src/main/java/dev/baranhan/viltrumiteflight/client/mixin/AbstractClientPlayerMixin.java:28: error: incompatible types: AbstractClientPlayerMixin cannot be converted to AbstractClientPlayer
      AbstractClientPlayer player = (AbstractClientPlayer)this;
                                                          ^
/home/runner/work/viltrumite-mods/viltrumite-mods/viltrumiteflight/src/main/java/dev/baranhan/viltrumiteflight/client/mixin/AbstractClientPlayerMixin.java:48: error: incompatible types: AbstractClientPlayerMixin cannot be converted to AbstractClientPlayer
      AbstractClientPlayer player = (AbstractClientPlayer)this;
                                                          ^
/home/runner/work/viltrumite-mods/viltrumite-mods/viltrumiteflight/src/main/java/dev/baranhan/viltrumiteflight/client/mixin/CapeHeatFeatureMixin.java:60: error: incompatible types: CapeHeatFeatureMixin cannot be converted to RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>>
               RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer = (RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>)this;
                                                                                                                                                                     ^
/home/runner/work/viltrumite-mods/viltrumite-mods/viltrumiteflight/src/main/java/dev/baranhan/viltrumiteflight/client/mixin/PlayerModelMixin.java:29: error: incompatible types: PlayerModelMixin<T> cannot be converted to PlayerModel<?>
      PlayerModel<?> playerModel = (PlayerModel<?>)this;
                                                   ^
  where T is a type-variable:
--
/home/runner/work/viltrumite-mods/viltrumite-mods/viltrumiteflight/src/main/java/dev/baranhan/viltrumiteflight/mixin/PlayerEntityMixin.java:305: error: incompatible types: PlayerEntityMixin cannot be converted to ServerPlayer
            if (this instanceof ServerPlayer serverPlayer && serverPlayer.isPassenger()) {
                ^
/home/runner/work/viltrumite-mods/viltrumite-mods/viltrumiteflight/src/main/java/dev/baranhan/viltrumiteflight/mixin/PlayerEntityMixin.java:321: error: incompatible types: PlayerEntityMixin cannot be converted to ServerPlayer
      if (!this.level().isClientSide() && this instanceof ServerPlayer serverPlayer) {
                                          ^
Note: Some input files use unchecked or unsafe operations.
* What went wrong:
Execution failed for task ':viltrumiteflight:compileJava'.
> Compilation failed; see the compiler error output for details.

* Try:
> Run with --info option to get more log output.
> Run with --scan to get full insights.

* Exception is:
org.gradle.api.tasks.TaskExecutionException: Execution failed for task ':viltrumiteflight:compileJava'.
	at org.gradle.api.internal.tasks.execution.ExecuteActionsTaskExecuter.lambda$executeIfValid$1(ExecuteActionsTaskExecuter.java:130)
	at org.gradle.internal.Try$Failure.ifSuccessfulOrElse(Try.java:282)
	at org.gradle.api.internal.tasks.execution.ExecuteActionsTaskExecuter.executeIfValid(ExecuteActionsTaskExecuter.java:128)
	at org.gradle.api.internal.tasks.execution.ExecuteActionsTaskExecuter.execute(ExecuteActionsTaskExecuter.java:116)
	at org.gradle.api.internal.tasks.execution.FinalizePropertiesTaskExecuter.execute(FinalizePropertiesTaskExecuter.java:46)
	at org.gradle.api.internal.tasks.execution.ResolveTaskExecutionModeExecuter.execute(ResolveTaskExecutionModeExecuter.java:51)
	at org.gradle.api.internal.tasks.execution.SkipTaskWithNoActionsExecuter.execute(SkipTaskWithNoActionsExecuter.java:57)
	at org.gradle.api.internal.tasks.execution.SkipOnlyIfTaskExecuter.execute(SkipOnlyIfTaskExecuter.java:74)
	at org.gradle.api.internal.tasks.execution.CatchExceptionTaskExecuter.execute(CatchExceptionTaskExecuter.java:36)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter$1.executeTask(EventFiringTaskExecuter.java:77)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter$1.call(EventFiringTaskExecuter.java:55)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter$1.call(EventFiringTaskExecuter.java:52)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$CallableBuildOperationWorker.execute(DefaultBuildOperationRunner.java:209)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$CallableBuildOperationWorker.execute(DefaultBuildOperationRunner.java:204)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$2.execute(DefaultBuildOperationRunner.java:66)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$2.execute(DefaultBuildOperationRunner.java:59)
```
