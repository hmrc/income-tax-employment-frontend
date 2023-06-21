
package support.builders.models.employment

import models.employment.{PayrollPaymentType, TaxableLumpSumItemModel, TaxableLumpSumViewModel}

object TaxableLumpSumDataBuilder {
val aTaxableLumpSumData: TaxableLumpSumViewModel = TaxableLumpSumViewModel(Seq(
  TaxableLumpSumItemModel(Some(100), Some(100), Some(PayrollPaymentType.AllPaid)),
  TaxableLumpSumItemModel(Some(99), Some(1), Some(PayrollPaymentType.SomePaid)),
  TaxableLumpSumItemModel(Some(98), None, Some(PayrollPaymentType.NonePaid)),
))
}
