data = read.table("results/anova_user_block_mean.txt", sep="\t", header = TRUE)
data$user = factor(data$user)
data$block = factor(data$block)
data$keyboard_type = factor(data$keyboard_type)

cat("\n========== Speed (WPM) ~ Keyboard * Block ==========\n")
with(data, tapply(wpm, list(keyboard_type, block), mean))
with(data, tapply(wpm, list(keyboard_type, block), sd))
aov.out = aov(wpm ~ keyboard_type * block + Error(user/(keyboard_type*block)), data=data)
summary(aov.out)
with(data, pairwise.t.test(x=wpm, g=keyboard_type, p.adjust.method="holm", paired=T))
with(data, pairwise.t.test(x=wpm, g=block, p.adjust.method="holm", paired=T))

cat("\n========== Word Error Rate (%) ~ Keyboard * Block ==========\n")
with(data, tapply(wer, list(keyboard_type, block), mean))
with(data, tapply(wer, list(keyboard_type, block), sd))
aov.out = aov(wer ~ keyboard_type * block + Error(user/(keyboard_type*block)), data=data)
summary(aov.out)
with(data, pairwise.t.test(x=wer, g=keyboard_type, p.adjust.method="holm", paired=T))
with(data, pairwise.t.test(x=wer, g=block, p.adjust.method="holm", paired=T))

cat("\n========== Backspace per Word ~ Keyboard * Block ==========\n")
with(data, tapply(delete_per_word, list(keyboard_type, block), mean))
with(data, tapply(delete_per_word, list(keyboard_type, block), sd))
aov.out = aov(delete_per_word ~ keyboard_type * block + Error(user/(keyboard_type*block)), data=data)
summary(aov.out)
with(data, pairwise.t.test(x=delete_per_word, g=keyboard_type, p.adjust.method="holm", paired=T))
with(data, pairwise.t.test(x=delete_per_word, g=block, p.adjust.method="holm", paired=T))



data = read.table("results/screen_time_user_block_mean.txt", sep="\t", header = TRUE)
data$user = factor(data$user)
data$block = factor(data$block)

cat("\n========== Screen on count ~ Block ==========\n")
with(data, tapply(screen_on_count, block, mean))
with(data, tapply(screen_on_count, block, sd))
aov.out = aov(screen_on_count ~ block + Error(user/(block)), data=data)
summary(aov.out)


cat("\n========== total_screen_on_duration ~ Block ==========\n")
with(data, tapply(total_screen_on_duration, block, mean))
with(data, tapply(total_screen_on_duration, block, sd))
aov.out = aov(total_screen_on_duration ~ block + Error(user/(block)), data=data)
summary(aov.out)
